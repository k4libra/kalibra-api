package com.kalibra.api.progress.application.internal.queryservices;

import com.kalibra.api.progress.application.internal.outboundservices.acl.ExternalCurriculumService;
import com.kalibra.api.progress.application.internal.outboundservices.acl.ExternalEnrollmentService;
import com.kalibra.api.progress.application.internal.outboundservices.cache.GapMapCacheService;
import com.kalibra.api.progress.domain.exceptions.CourseNotOwnedByTeacherException;
import com.kalibra.api.progress.domain.exceptions.NoIndicatorsAvailableException;
import com.kalibra.api.progress.domain.exceptions.NotEnrolledInCourseException;
import com.kalibra.api.progress.domain.model.aggregates.ExerciseAttempt;
import com.kalibra.api.progress.domain.model.aggregates.SubtopicMastery;
import com.kalibra.api.progress.domain.model.queries.ExportCourseIndicatorsQuery;
import com.kalibra.api.progress.domain.model.queries.GetCourseIndicatorsQuery;
import com.kalibra.api.progress.domain.model.queries.GetIndicatorGuideQuery;
import com.kalibra.api.progress.domain.model.queries.GetMasteryGapMapByCourseQuery;
import com.kalibra.api.progress.domain.model.queries.GetPracticeSubtopicsByCourseQuery;
import com.kalibra.api.progress.domain.model.queries.GetStudentProgressByCourseQuery;
import com.kalibra.api.progress.domain.model.queries.GetStudentProgressForTeacherQuery;
import com.kalibra.api.progress.domain.model.valueobjects.AccuracyIndicator;
import com.kalibra.api.progress.domain.model.valueobjects.CourseId;
import com.kalibra.api.progress.domain.model.valueobjects.CourseIndicatorsReport;
import com.kalibra.api.progress.domain.model.valueobjects.IndicatorExportRow;
import com.kalibra.api.progress.domain.model.valueobjects.IndicatorGuide;
import com.kalibra.api.progress.domain.model.valueobjects.IndicatorsCsvExport;
import com.kalibra.api.progress.domain.model.valueobjects.MasteryEvolutionIndicator;
import com.kalibra.api.progress.domain.model.valueobjects.MasteryGapMap;
import com.kalibra.api.progress.domain.model.valueobjects.MasteryLevel;
import com.kalibra.api.progress.domain.model.valueobjects.PracticeIndicator;
import com.kalibra.api.progress.domain.model.valueobjects.PracticeSubtopicView;
import com.kalibra.api.progress.domain.model.valueobjects.StudentAccuracyLine;
import com.kalibra.api.progress.domain.model.valueobjects.StudentId;
import com.kalibra.api.progress.domain.model.valueobjects.StudentMasteryCell;
import com.kalibra.api.progress.domain.model.valueobjects.StudentProgressReport;
import com.kalibra.api.progress.domain.model.valueobjects.SubtopicApprovalLine;
import com.kalibra.api.progress.domain.model.valueobjects.SubtopicEvolutionLine;
import com.kalibra.api.progress.domain.model.valueobjects.SubtopicGapLine;
import com.kalibra.api.progress.domain.model.valueobjects.SubtopicPracticeLine;
import com.kalibra.api.progress.domain.model.valueobjects.SubtopicProgressLine;
import com.kalibra.api.progress.domain.model.valueobjects.VerificationIndicator;
import com.kalibra.api.progress.domain.repositories.ExerciseAttemptRepository;
import com.kalibra.api.progress.domain.repositories.SubtopicMasteryRepository;
import com.kalibra.api.progress.domain.services.CourseIndicatorsCalculator;
import com.kalibra.api.progress.domain.services.MasteryGapAnalyzer;
import com.kalibra.api.progress.domain.services.StudentAnonymizer;
import com.kalibra.api.progress.domain.services.SubtopicMasteryQueryService;
import com.kalibra.api.shared.contracts.curriculum.SubtopicSummary;
import com.kalibra.api.shared.contracts.curriculum.SubtopicVerificationStats;
import com.kalibra.api.shared.contracts.enrollment.RosterEntry;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class SubtopicMasteryQueryServiceImpl
        implements SubtopicMasteryQueryService {

    private static final int RECENT_FEEDBACK = 5;

    private final SubtopicMasteryRepository subtopicMasteryRepository;
    private final ExerciseAttemptRepository exerciseAttemptRepository;
    private final ExternalCurriculumService externalCurriculumService;
    private final ExternalEnrollmentService externalEnrollmentService;
    private final GapMapCacheService gapMapCacheService;
    private final MasteryGapAnalyzer masteryGapAnalyzer = new MasteryGapAnalyzer();
    private final CourseIndicatorsCalculator courseIndicatorsCalculator = new CourseIndicatorsCalculator();
    private final StudentAnonymizer studentAnonymizer = new StudentAnonymizer();

    public SubtopicMasteryQueryServiceImpl(
            SubtopicMasteryRepository subtopicMasteryRepository,
            ExerciseAttemptRepository exerciseAttemptRepository,
            ExternalCurriculumService externalCurriculumService,
            ExternalEnrollmentService externalEnrollmentService,
            GapMapCacheService gapMapCacheService) {
        this.subtopicMasteryRepository = subtopicMasteryRepository;
        this.exerciseAttemptRepository = exerciseAttemptRepository;
        this.externalCurriculumService = externalCurriculumService;
        this.externalEnrollmentService = externalEnrollmentService;
        this.gapMapCacheService = gapMapCacheService;
    }

    @Override
    public List<PracticeSubtopicView> handle(
            GetPracticeSubtopicsByCourseQuery query) {
        requireEnrollment(query.holderId(), query.courseId());
        var levels = subtopicMasteryRepository
                .findAllByHolderIdAndCourseId(query.holderId(), query.courseId())
                .stream()
                .collect(Collectors.toMap(
                        mastery -> mastery.getSubtopicId().value(),
                        SubtopicMastery::getLevel,
                        (first, second) -> second));
        return externalCurriculumService
                .fetchSubtopics(query.courseId())
                .stream()
                .map(subtopic -> new PracticeSubtopicView(
                        subtopic.subtopicId(),
                        subtopic.name(),
                        levels.getOrDefault(subtopic.subtopicId(), MasteryLevel.NO_DATA)
                ))
                .toList();
    }

    @Override
    public StudentProgressReport handle(
            GetStudentProgressByCourseQuery query) {
        var studentId = requireEnrollment(query.holderId(), query.courseId());
        return progressOf(studentId, query.courseId());
    }

    @Override
    public StudentProgressReport handle(
            GetStudentProgressForTeacherQuery query) {
        requireOwnership(query.holderId(), query.courseId());
        if (!externalEnrollmentService.isEnrolled(query.studentId(), query.courseId())) {
            throw new NotEnrolledInCourseException();
        }
        return progressOf(query.studentId(), query.courseId());
    }

    @Override
    public MasteryGapMap handle(GetMasteryGapMapByCourseQuery query) {
        requireOwnership(query.holderId(), query.courseId());
        var roster = externalEnrollmentService.fetchRosterEntries(query.courseId());
        // Self-healing: a cached map is only served while it still covers the current roster.
        var cached = gapMapCacheService
                .find(query.courseId())
                .filter(map -> coversRoster(map, roster));
        if (cached.isPresent()) {
            return cached.get();
        }
        var map = gapMapOf(query.courseId(), roster);
        gapMapCacheService.store(map);
        return map;
    }

    @Override
    public CourseIndicatorsReport handle(GetCourseIndicatorsQuery query) {
        requireOwnership(query.holderId(), query.courseId());
        var attempts = exerciseAttemptRepository.findAllByCourseId(query.courseId());
        var masteries = subtopicMasteryRepository.findAllByCourseId(query.courseId());
        var calculated = courseIndicatorsCalculator.calculate(attempts, masteries);
        var subtopics = externalCurriculumService.fetchSubtopics(query.courseId());
        var roster = externalEnrollmentService.fetchRosterEntries(query.courseId());
        return new CourseIndicatorsReport(
                query.courseId().value(),
                calculated.hasActivity(),
                complete(calculated.accuracy(), roster),
                complete(calculated.practice(), subtopics, roster),
                complete(calculated.evolution(), subtopics),
                verificationOf(externalCurriculumService.fetchVerificationStats(query.courseId()))
        );
    }

    @Override
    public IndicatorsCsvExport handle(ExportCourseIndicatorsQuery query) {
        requireOwnership(query.holderId(), query.courseId());
        var attempts = exerciseAttemptRepository.findAllByCourseId(query.courseId());
        if (attempts.isEmpty()) {
            throw new NoIndicatorsAvailableException(query.courseId().value());
        }
        var masteries = subtopicMasteryRepository.findAllByCourseId(query.courseId());
        var subtopics = externalCurriculumService.fetchSubtopics(query.courseId());
        Map<UUID, Double> approvalRates = externalCurriculumService
                .fetchVerificationStats(query.courseId())
                .stream()
                .collect(Collectors.toMap(
                        SubtopicVerificationStats::subtopicId,
                        stats -> CourseIndicatorsCalculator.percentage(stats.approved(), stats.generated()),
                        (first, second) -> second));
        var rows = new ArrayList<IndicatorExportRow>();
        var holders = attempts.stream().map(ExerciseAttempt::getHolderId).distinct().toList();
        for (var holderId : holders) {
            var code = studentAnonymizer.codeFor(new StudentId(UUID.fromString(holderId)));
            for (var subtopic : subtopics) {
                var answered = attempts.stream()
                        .filter(attempt -> attempt.getHolderId().equals(holderId)
                                && attempt.getSubtopicId().value().equals(subtopic.subtopicId()))
                        .sorted(Comparator.comparing(ExerciseAttempt::getAnsweredAt))
                        .toList();
                if (answered.isEmpty()) {
                    continue;
                }
                var mastery = masteries.stream()
                        .filter(candidate -> candidate.getHolderId().equals(holderId)
                                && candidate.getSubtopicId().value().equals(subtopic.subtopicId()))
                        .findFirst();
                var correct = answered.stream().filter(ExerciseAttempt::isCorrect).count();
                rows.add(new IndicatorExportRow(
                        code,
                        subtopic.name(),
                        answered.size(),
                        CourseIndicatorsCalculator.percentage(correct, answered.size()),
                        mastery.map(found -> found.getInitialEstimate().asPercentage())
                                .orElseGet(() -> answered.getFirst().getMasteryChange().current().asPercentage()),
                        mastery.map(found -> found.getCurrentEstimate().asPercentage())
                                .orElseGet(() -> answered.getLast().getMasteryChange().current().asPercentage()),
                        approvalRates.getOrDefault(subtopic.subtopicId(), 0.0)
                ));
            }
        }
        rows.sort(Comparator.comparing(row -> row.studentCode().value()));
        return new IndicatorsCsvExport("course-indicators-" + query.courseId().value() + ".csv", rows);
    }

    @Override
    public IndicatorGuide handle(GetIndicatorGuideQuery query) {
        return IndicatorGuide.standard();
    }

    private StudentId requireEnrollment(String holderId, CourseId courseId) {
        StudentId studentId;
        try {
            studentId = new StudentId(UUID.fromString(holderId));
        } catch (IllegalArgumentException notAStudentId) {
            throw new NotEnrolledInCourseException();
        }
        if (!externalEnrollmentService.isEnrolled(studentId, courseId)) {
            throw new NotEnrolledInCourseException();
        }
        return studentId;
    }

    private void requireOwnership(String holderId, CourseId courseId) {
        if (!externalCurriculumService.isCourseOwnedBy(courseId, holderId)) {
            throw new CourseNotOwnedByTeacherException(courseId.value());
        }
    }

    private StudentProgressReport progressOf(StudentId studentId, CourseId courseId) {
        var holderId = studentId.value().toString();
        var masteries = subtopicMasteryRepository.findAllByHolderIdAndCourseId(holderId, courseId);
        var attempts = exerciseAttemptRepository.findAllByHolderIdAndCourseId(holderId, courseId);
        var lines = externalCurriculumService
                .fetchSubtopics(courseId)
                .stream()
                .map(subtopic -> {
                    var mastery = masteries.stream()
                            .filter(candidate -> candidate.getSubtopicId().value().equals(subtopic.subtopicId()))
                            .findFirst();
                    var solved = (int) attempts.stream()
                            .filter(attempt -> attempt.getSubtopicId().value().equals(subtopic.subtopicId()))
                            .count();
                    return new SubtopicProgressLine(
                            subtopic.subtopicId(),
                            subtopic.name(),
                            mastery.map(SubtopicMastery::getCurrentEstimate),
                            mastery.map(SubtopicMastery::getLevel).orElse(MasteryLevel.NO_DATA),
                            solved
                    );
                })
                .toList();
        var recentFeedback = attempts.stream()
                .sorted(Comparator.comparing(ExerciseAttempt::getAnsweredAt).reversed())
                .limit(RECENT_FEEDBACK)
                .map(ExerciseAttempt::getFeedback)
                .toList();
        return new StudentProgressReport(
                studentId.value(),
                courseId.value(),
                !attempts.isEmpty() || !masteries.isEmpty(),
                lines,
                recentFeedback
        );
    }

    private boolean coversRoster(MasteryGapMap map, List<RosterEntry> roster) {
        Set<UUID> cachedStudents = map.students().stream()
                .map(StudentMasteryCell::studentId)
                .collect(Collectors.toSet());
        Set<UUID> enrolled = roster.stream()
                .map(RosterEntry::studentId)
                .collect(Collectors.toSet());
        return cachedStudents.equals(enrolled);
    }

    private MasteryGapMap gapMapOf(CourseId courseId, List<RosterEntry> roster) {
        var students = roster.stream().map(entry -> new StudentId(entry.studentId())).toList();
        var analyzed = masteryGapAnalyzer.analyze(subtopicMasteryRepository.findAllByCourseId(courseId), students);
        var subtopics = externalCurriculumService.fetchSubtopics(courseId);
        Map<UUID, SubtopicSummary> known = subtopics.stream()
                .collect(Collectors.toMap(SubtopicSummary::subtopicId, Function.identity(), (first, second) -> first));
        Map<UUID, String> emails = roster.stream()
                .collect(Collectors.toMap(RosterEntry::studentId, RosterEntry::email, (first, second) -> first));

        var lines = new ArrayList<SubtopicGapLine>();
        var cells = new ArrayList<StudentMasteryCell>();
        for (var line : analyzed.subtopics()) {
            var subtopic = known.get(line.subtopicId());
            if (subtopic != null) {
                lines.add(new SubtopicGapLine(line.subtopicId(), subtopic.name(), line.groupMastery(), line.lowCount(),
                        line.mediumCount(), line.highCount(), line.noDataCount(), lines.size() + 1));
            }
        }
        for (var cell : analyzed.students()) {
            if (known.containsKey(cell.subtopicId())) {
                cells.add(new StudentMasteryCell(cell.studentId(), emails.get(cell.studentId()), cell.subtopicId(),
                        cell.mastery(), cell.level()));
            }
        }
        var withData = lines.stream().map(SubtopicGapLine::subtopicId).collect(Collectors.toSet());
        for (var subtopic : subtopics) {
            if (withData.contains(subtopic.subtopicId())) {
                continue;
            }
            lines.add(new SubtopicGapLine(subtopic.subtopicId(), subtopic.name(), 0, 0, 0, 0,
                    roster.size(), lines.size() + 1));
            for (var entry : roster) {
                cells.add(new StudentMasteryCell(entry.studentId(), entry.email(), subtopic.subtopicId(),
                        Optional.empty(), MasteryLevel.NO_DATA));
            }
        }
        return new MasteryGapMap(courseId.value(), analyzed.hasSufficientData(), lines, cells);
    }

    private AccuracyIndicator complete(AccuracyIndicator accuracy, List<RosterEntry> roster) {
        Map<UUID, String> emails = roster.stream()
                .collect(Collectors.toMap(RosterEntry::studentId, RosterEntry::email, (first, second) -> first));
        var lines = new ArrayList<StudentAccuracyLine>();
        for (var line : accuracy.perStudent()) {
            lines.add(new StudentAccuracyLine(line.studentId(), emails.get(line.studentId()), line.correct(),
                    line.submitted(), line.accuracy(), true));
        }
        var active = lines.stream().map(StudentAccuracyLine::studentId).collect(Collectors.toSet());
        for (var entry : roster) {
            if (!active.contains(entry.studentId())) {
                lines.add(new StudentAccuracyLine(entry.studentId(), entry.email(), 0, 0, Optional.empty(), false));
            }
        }
        return new AccuracyIndicator(accuracy.groupAccuracy(), lines);
    }

    private PracticeIndicator complete(PracticeIndicator practice, List<SubtopicSummary> subtopics, List<RosterEntry> roster) {
        Map<UUID, Integer> solved = practice.perSubtopic().stream()
                .collect(Collectors.toMap(SubtopicPracticeLine::subtopicId, SubtopicPracticeLine::solved, Integer::sum));
        var lines = subtopics.stream()
                .map(subtopic -> new SubtopicPracticeLine(subtopic.subtopicId(), subtopic.name(),
                        solved.getOrDefault(subtopic.subtopicId(), 0)))
                .toList();
        return new PracticeIndicator(practice.totalSolved(), practice.averagePerActiveStudent(), practice.activeStudents(),
                Math.max(roster.size(), practice.activeStudents()), lines);
    }

    private MasteryEvolutionIndicator complete(MasteryEvolutionIndicator evolution, List<SubtopicSummary> subtopics) {
        Map<UUID, SubtopicEvolutionLine> practiced = evolution.perSubtopic().stream()
                .collect(Collectors.toMap(SubtopicEvolutionLine::subtopicId, Function.identity(), (first, second) -> first));
        var lines = subtopics.stream()
                .map(subtopic -> Optional.ofNullable(practiced.get(subtopic.subtopicId()))
                        .map(line -> new SubtopicEvolutionLine(line.subtopicId(), subtopic.name(), line.initialAverage(),
                                line.currentAverage(), line.deltaPoints(), line.level(), true))
                        .orElseGet(() -> new SubtopicEvolutionLine(subtopic.subtopicId(), subtopic.name(),
                                Optional.empty(), Optional.empty(), Optional.empty(), MasteryLevel.NO_DATA, false)))
                .toList();
        return new MasteryEvolutionIndicator(evolution.groupInitial(), evolution.groupCurrent(),
                evolution.groupDeltaPoints(), lines);
    }

    private VerificationIndicator verificationOf(List<SubtopicVerificationStats> stats) {
        var generated = stats.stream().mapToInt(SubtopicVerificationStats::generated).sum();
        var approved = stats.stream().mapToInt(SubtopicVerificationStats::approved).sum();
        var lines = stats.stream()
                .filter(subtopic -> subtopic.generated() > 0)
                .map(subtopic -> new SubtopicApprovalLine(subtopic.subtopicId(), subtopic.subtopicName(),
                        CourseIndicatorsCalculator.percentage(subtopic.approved(), subtopic.generated())))
                .toList();
        return new VerificationIndicator(CourseIndicatorsCalculator.percentage(approved, generated), approved,
                generated - approved, lines);
    }
}
