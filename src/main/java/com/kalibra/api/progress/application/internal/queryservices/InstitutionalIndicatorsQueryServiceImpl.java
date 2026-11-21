package com.kalibra.api.progress.application.internal.queryservices;

import com.kalibra.api.progress.application.internal.outboundservices.acl.ExternalCurriculumService;
import com.kalibra.api.progress.application.internal.outboundservices.acl.ExternalEnrollmentService;
import com.kalibra.api.progress.application.internal.outboundservices.acl.ExternalIamService;
import com.kalibra.api.progress.domain.model.aggregates.ExerciseAttempt;
import com.kalibra.api.progress.domain.model.queries.GetCriticalSubtopicsQuery;
import com.kalibra.api.progress.domain.model.queries.GetInstitutionalIndicatorsQuery;
import com.kalibra.api.progress.domain.model.valueobjects.CourseId;
import com.kalibra.api.progress.domain.model.valueobjects.CourseIndicatorSummary;
import com.kalibra.api.progress.domain.model.valueobjects.CriticalSubtopicLine;
import com.kalibra.api.progress.domain.model.valueobjects.InstitutionalIndicatorTotals;
import com.kalibra.api.progress.domain.model.valueobjects.InstitutionalIndicators;
import com.kalibra.api.progress.domain.model.valueobjects.StudentId;
import com.kalibra.api.progress.domain.repositories.ExerciseAttemptRepository;
import com.kalibra.api.progress.domain.repositories.SubtopicMasteryRepository;
import com.kalibra.api.progress.domain.services.CourseIndicatorsCalculator;
import com.kalibra.api.progress.domain.services.InstitutionalIndicatorsQueryService;
import com.kalibra.api.progress.domain.services.MasteryGapAnalyzer;
import com.kalibra.api.shared.contracts.curriculum.SubtopicSummary;
import com.kalibra.api.shared.contracts.curriculum.SubtopicVerificationStats;
import com.kalibra.api.shared.contracts.enrollment.RosterEntry;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

// Read-only, platform-wide view for administrators: every course, with no holder scoping.
// Only aggregates leave this service; no student identity is exposed.
@Service
public class InstitutionalIndicatorsQueryServiceImpl implements InstitutionalIndicatorsQueryService {

    private static final int CRITICAL_SUBTOPICS_LIMIT = 20;

    private final ExerciseAttemptRepository exerciseAttemptRepository;
    private final SubtopicMasteryRepository subtopicMasteryRepository;
    private final ExternalCurriculumService externalCurriculumService;
    private final ExternalEnrollmentService externalEnrollmentService;
    private final ExternalIamService externalIamService;
    private final CourseIndicatorsCalculator courseIndicatorsCalculator = new CourseIndicatorsCalculator();
    private final MasteryGapAnalyzer masteryGapAnalyzer = new MasteryGapAnalyzer();

    public InstitutionalIndicatorsQueryServiceImpl(ExerciseAttemptRepository exerciseAttemptRepository,
                                                   SubtopicMasteryRepository subtopicMasteryRepository,
                                                   ExternalCurriculumService externalCurriculumService,
                                                   ExternalEnrollmentService externalEnrollmentService,
                                                   ExternalIamService externalIamService) {
        this.exerciseAttemptRepository = exerciseAttemptRepository;
        this.subtopicMasteryRepository = subtopicMasteryRepository;
        this.externalCurriculumService = externalCurriculumService;
        this.externalEnrollmentService = externalEnrollmentService;
        this.externalIamService = externalIamService;
    }

    @Override
    public InstitutionalIndicators handle(GetInstitutionalIndicatorsQuery query) {
        var summaries = new ArrayList<CourseIndicatorSummary>();
        var teachers = new HashSet<String>();
        var enrolled = new HashSet<UUID>();
        var active = new HashSet<String>();
        long correct = 0;
        long submitted = 0;
        long approved = 0;
        long generated = 0;
        for (var course : externalCurriculumService.fetchAllCourses()) {
            var courseId = new CourseId(course.courseId());
            var attempts = exerciseAttemptRepository.findAllByCourseId(courseId);
            var report = courseIndicatorsCalculator.calculate(attempts,
                    subtopicMasteryRepository.findAllByCourseId(courseId));
            var roster = externalEnrollmentService.fetchRosterEntries(courseId);
            var stats = externalCurriculumService.fetchVerificationStats(courseId);
            var courseApproved = stats.stream().mapToInt(SubtopicVerificationStats::approved).sum();
            var courseGenerated = stats.stream().mapToInt(SubtopicVerificationStats::generated).sum();
            var courseActive = attempts.stream().map(ExerciseAttempt::getHolderId).distinct().count();

            teachers.add(course.holderId());
            roster.forEach(entry -> enrolled.add(entry.studentId()));
            attempts.forEach(attempt -> active.add(attempt.getHolderId()));
            correct += attempts.stream().filter(ExerciseAttempt::isCorrect).count();
            submitted += attempts.size();
            approved += courseApproved;
            generated += courseGenerated;

            summaries.add(new CourseIndicatorSummary(
                    course.courseId(),
                    course.name(),
                    course.code(),
                    externalIamService.fetchFullName(course.holderId()).orElse(null),
                    report.hasActivity(),
                    roster.size(),
                    (int) courseActive,
                    attempts.size(),
                    report.accuracy().groupAccuracy(),
                    report.evolution().groupDeltaPoints(),
                    CourseIndicatorsCalculator.percentage(courseApproved, courseGenerated)
            ));
        }
        var withActivity = summaries.stream().filter(CourseIndicatorSummary::hasActivity).toList();
        var groupDelta = (int) Math.round(withActivity.stream()
                .mapToInt(CourseIndicatorSummary::groupDeltaPoints).average().orElse(0));
        var totals = new InstitutionalIndicatorTotals(
                summaries.size(),
                teachers.size(),
                enrolled.size(),
                active.size(),
                (int) submitted,
                CourseIndicatorsCalculator.percentage(correct, submitted),
                groupDelta,
                CourseIndicatorsCalculator.percentage(approved, generated)
        );
        return new InstitutionalIndicators(totals, summaries);
    }

    // Weakest subtopics across every course that already has mastery data: lowest group mastery
    // first and, on a tie, the one with more students at a low level.
    @Override
    public List<CriticalSubtopicLine> handle(GetCriticalSubtopicsQuery query) {
        var lines = new ArrayList<CriticalSubtopicLine>();
        for (var course : externalCurriculumService.fetchAllCourses()) {
            var courseId = new CourseId(course.courseId());
            var roster = externalEnrollmentService.fetchRosterEntries(courseId).stream()
                    .map(RosterEntry::studentId)
                    .map(StudentId::new)
                    .toList();
            Map<UUID, SubtopicSummary> subtopics = externalCurriculumService.fetchSubtopics(courseId).stream()
                    .collect(Collectors.toMap(SubtopicSummary::subtopicId, Function.identity(), (first, second) -> first));
            var analyzed = masteryGapAnalyzer.analyze(subtopicMasteryRepository.findAllByCourseId(courseId), roster);
            for (var line : analyzed.subtopics()) {
                var subtopic = subtopics.get(line.subtopicId());
                if (subtopic == null || line.lowCount() + line.mediumCount() + line.highCount() == 0) {
                    continue;
                }
                lines.add(new CriticalSubtopicLine(course.courseId(), course.name(), line.subtopicId(),
                        subtopic.name(), line.groupMastery(), line.lowCount(), line.mediumCount(),
                        line.highCount(), line.noDataCount()));
            }
        }
        return lines.stream()
                .sorted(Comparator.comparingDouble(CriticalSubtopicLine::groupMastery)
                        .thenComparing(Comparator.comparingInt(CriticalSubtopicLine::lowCount).reversed()))
                .limit(CRITICAL_SUBTOPICS_LIMIT)
                .toList();
    }
}
