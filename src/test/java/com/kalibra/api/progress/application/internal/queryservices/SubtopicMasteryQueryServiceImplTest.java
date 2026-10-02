package com.kalibra.api.progress.application.internal.queryservices;

import com.kalibra.api.progress.ProgressFixtures;
import com.kalibra.api.progress.application.internal.outboundservices.acl.ExternalCurriculumService;
import com.kalibra.api.progress.application.internal.outboundservices.acl.ExternalEnrollmentService;
import com.kalibra.api.progress.application.internal.outboundservices.cache.GapMapCacheService;
import com.kalibra.api.progress.domain.exceptions.CourseNotOwnedByTeacherException;
import com.kalibra.api.progress.domain.exceptions.NoIndicatorsAvailableException;
import com.kalibra.api.progress.domain.exceptions.NotEnrolledInCourseException;
import com.kalibra.api.progress.domain.model.queries.ExportCourseIndicatorsQuery;
import com.kalibra.api.progress.domain.model.queries.GetCourseIndicatorsQuery;
import com.kalibra.api.progress.domain.model.queries.GetIndicatorGuideQuery;
import com.kalibra.api.progress.domain.model.queries.GetMasteryGapMapByCourseQuery;
import com.kalibra.api.progress.domain.model.queries.GetPracticeSubtopicsByCourseQuery;
import com.kalibra.api.progress.domain.model.queries.GetStudentProgressByCourseQuery;
import com.kalibra.api.progress.domain.model.queries.GetStudentProgressForTeacherQuery;
import com.kalibra.api.progress.domain.model.valueobjects.CourseId;
import com.kalibra.api.progress.domain.model.valueobjects.MasteryGapMap;
import com.kalibra.api.progress.domain.model.valueobjects.MasteryLevel;
import com.kalibra.api.progress.domain.model.valueobjects.PracticeSubtopicView;
import com.kalibra.api.progress.domain.model.valueobjects.StudentId;
import com.kalibra.api.progress.domain.model.valueobjects.StudentMasteryCell;
import com.kalibra.api.progress.domain.model.valueobjects.SubtopicGapLine;
import com.kalibra.api.progress.domain.model.valueobjects.SubtopicId;
import com.kalibra.api.progress.domain.repositories.ExerciseAttemptRepository;
import com.kalibra.api.progress.domain.repositories.SubtopicMasteryRepository;
import com.kalibra.api.progress.domain.services.StudentAnonymizer;
import com.kalibra.api.shared.contracts.curriculum.SubtopicSummary;
import com.kalibra.api.shared.contracts.curriculum.SubtopicVerificationStats;
import com.kalibra.api.shared.contracts.enrollment.RosterEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubtopicMasteryQueryServiceImplTest {

    private static final String TEACHER = "teacher-1";

    @Mock
    SubtopicMasteryRepository subtopicMasteryRepository;

    @Mock
    ExerciseAttemptRepository exerciseAttemptRepository;

    @Mock
    ExternalCurriculumService externalCurriculumService;

    @Mock
    ExternalEnrollmentService externalEnrollmentService;

    @Mock
    GapMapCacheService gapMapCacheService;

    @InjectMocks
    SubtopicMasteryQueryServiceImpl service;

    private final CourseId courseId = new CourseId(UUID.randomUUID());
    private final SubtopicId equations = new SubtopicId(UUID.randomUUID());
    private final SubtopicId inequalities = new SubtopicId(UUID.randomUUID());
    private final StudentId ana = new StudentId(UUID.randomUUID());
    private final StudentId luis = new StudentId(UUID.randomUUID());
    private final String anaHolder = ana.value().toString();
    private final List<RosterEntry> roster = List.of(
            new RosterEntry(ana.value(), "ana@kalibra.pe", Instant.now()),
            new RosterEntry(luis.value(), "luis@kalibra.pe", Instant.now()));

    @BeforeEach
    void setUp() {
        lenient().when(externalCurriculumService.fetchSubtopics(courseId)).thenReturn(List.of(
                new SubtopicSummary(equations.value(), "Equations", 1),
                new SubtopicSummary(inequalities.value(), "Inequalities", 2)));
        lenient().when(externalCurriculumService.isCourseOwnedBy(courseId, TEACHER)).thenReturn(true);
        lenient().when(externalEnrollmentService.fetchRosterEntries(courseId)).thenReturn(roster);
        lenient().when(externalEnrollmentService.isEnrolled(ana, courseId)).thenReturn(true);
    }

    @Test
    void shouldListTheSubtopicsOfTheCourseWithTheLevelOfTheStudent() {
        when(subtopicMasteryRepository.findAllByHolderIdAndCourseId(anaHolder, courseId)).thenReturn(List.of(
                ProgressFixtures.mastery(anaHolder, courseId, equations, 0.30, 0.74, MasteryLevel.HIGH)));

        var subtopics = service.handle(new GetPracticeSubtopicsByCourseQuery(anaHolder, courseId));

        assertThat(subtopics).containsExactly(
                new PracticeSubtopicView(equations.value(), "Equations", MasteryLevel.HIGH),
                new PracticeSubtopicView(inequalities.value(), "Inequalities", MasteryLevel.NO_DATA));
    }

    @Test
    void shouldHideTheSubtopicsFromAStudentWhoIsNotEnrolled() {
        var outsider = UUID.randomUUID().toString();

        assertThatThrownBy(() -> service.handle(new GetPracticeSubtopicsByCourseQuery(outsider, courseId)))
                .isInstanceOf(NotEnrolledInCourseException.class);
        assertThatThrownBy(() -> service.handle(new GetPracticeSubtopicsByCourseQuery("teacher-1", courseId)))
                .isInstanceOf(NotEnrolledInCourseException.class);
        verifyNoInteractions(subtopicMasteryRepository);
    }

    @Test
    void shouldReportTheProgressOfTheStudentWithMasterySolvedCountAndRecentFeedback() {
        // Arrange
        when(subtopicMasteryRepository.findAllByHolderIdAndCourseId(anaHolder, courseId)).thenReturn(List.of(
                ProgressFixtures.mastery(anaHolder, courseId, equations, 0.30, 0.55, MasteryLevel.MEDIUM)));
        when(exerciseAttemptRepository.findAllByHolderIdAndCourseId(anaHolder, courseId)).thenReturn(List.of(
                ProgressFixtures.attempt(anaHolder, courseId, equations, "A", 0.30, 0.21),
                ProgressFixtures.attempt(anaHolder, courseId, equations, "B", 0.21, 0.55)));

        // Act
        var report = service.handle(new GetStudentProgressByCourseQuery(anaHolder, courseId));

        // Assert
        assertThat(report.hasActivity()).isTrue();
        assertThat(report.studentId()).isEqualTo(ana.value());
        assertThat(report.subtopics()).hasSize(2);
        assertThat(report.subtopics().getFirst().subtopicName()).isEqualTo("Equations");
        assertThat(report.subtopics().getFirst().mastery()).isPresent();
        assertThat(report.subtopics().getFirst().mastery().get().asPercentage()).isEqualTo(55);
        assertThat(report.subtopics().getFirst().level()).isEqualTo(MasteryLevel.MEDIUM);
        assertThat(report.subtopics().getFirst().solvedCount()).isEqualTo(2);
        assertThat(report.subtopics().get(1).mastery()).isEmpty();
        assertThat(report.subtopics().get(1).level()).isEqualTo(MasteryLevel.NO_DATA);
        assertThat(report.subtopics().get(1).solvedCount()).isZero();
        assertThat(report.recentFeedback()).hasSize(2)
                .allSatisfy(feedback -> assertThat(feedback.explanation()).isEqualTo(ProgressFixtures.EXPLANATION));
    }

    @Test
    void shouldReportAnInitialStateForAStudentWithoutSolvedExercises() {
        when(subtopicMasteryRepository.findAllByHolderIdAndCourseId(anaHolder, courseId)).thenReturn(List.of());
        when(exerciseAttemptRepository.findAllByHolderIdAndCourseId(anaHolder, courseId)).thenReturn(List.of());

        var report = service.handle(new GetStudentProgressByCourseQuery(anaHolder, courseId));

        assertThat(report.hasActivity()).isFalse();
        assertThat(report.recentFeedback()).isEmpty();
        assertThat(report.subtopics()).hasSize(2)
                .allSatisfy(line -> {
                    assertThat(line.mastery()).isEmpty();
                    assertThat(line.level()).isEqualTo(MasteryLevel.NO_DATA);
                });
    }

    @Test
    void shouldLetTheTeacherSeeTheProgressOfAnEnrolledStudentOfTheirCourse() {
        when(subtopicMasteryRepository.findAllByHolderIdAndCourseId(anaHolder, courseId)).thenReturn(List.of());
        when(exerciseAttemptRepository.findAllByHolderIdAndCourseId(anaHolder, courseId)).thenReturn(List.of());

        var report = service.handle(new GetStudentProgressForTeacherQuery(TEACHER, courseId, ana));

        assertThat(report.studentId()).isEqualTo(ana.value());
        assertThat(report.hasActivity()).isFalse();
        assertThat(report.subtopics()).hasSize(2);
    }

    @Test
    void shouldHideTheStudentProgressFromATeacherWhoDoesNotOwnTheCourseOrForAStudentOutsideIt() {
        var outsider = new StudentId(UUID.randomUUID());

        assertThatThrownBy(() -> service.handle(new GetStudentProgressForTeacherQuery("teacher-2", courseId, ana)))
                .isInstanceOf(CourseNotOwnedByTeacherException.class);
        assertThatThrownBy(() -> service.handle(new GetStudentProgressForTeacherQuery(TEACHER, courseId, outsider)))
                .isInstanceOf(NotEnrolledInCourseException.class);
        verifyNoInteractions(subtopicMasteryRepository, exerciseAttemptRepository);
    }

    @Test
    void shouldBuildTheGapMapWithNamesEmailsAndSubtopicsWithoutDataLastAndCacheIt() {
        // Arrange
        when(gapMapCacheService.find(courseId)).thenReturn(Optional.empty());
        when(subtopicMasteryRepository.findAllByCourseId(courseId)).thenReturn(List.of(
                ProgressFixtures.mastery(anaHolder, courseId, inequalities, 0.30, 0.25, MasteryLevel.LOW)));

        // Act
        var map = service.handle(new GetMasteryGapMapByCourseQuery(TEACHER, courseId));

        // Assert
        assertThat(map.courseId()).isEqualTo(courseId.value());
        assertThat(map.hasSufficientData()).isTrue();
        assertThat(map.subtopics()).extracting(SubtopicGapLine::subtopicName).containsExactly("Inequalities", "Equations");
        assertThat(map.subtopics()).extracting(SubtopicGapLine::reinforcementPriority).containsExactly(1, 2);
        assertThat(map.subtopics().getFirst().groupMastery()).isEqualTo(25.0);
        assertThat(map.subtopics().getFirst().lowCount()).isEqualTo(1);
        assertThat(map.subtopics().getFirst().noDataCount()).isEqualTo(1);
        assertThat(map.subtopics().get(1).noDataCount()).isEqualTo(2);
        assertThat(map.students()).hasSize(4);
        assertThat(map.students()).extracting(StudentMasteryCell::email).containsOnly("ana@kalibra.pe", "luis@kalibra.pe");
        assertThat(map.students()).filteredOn(cell -> cell.level() != MasteryLevel.NO_DATA).singleElement()
                .satisfies(cell -> {
                    assertThat(cell.studentId()).isEqualTo(ana.value());
                    assertThat(cell.subtopicId()).isEqualTo(inequalities.value());
                });
        verify(gapMapCacheService).store(map);
    }

    @Test
    void shouldReportInsufficientDataForACourseWithoutEstimates() {
        when(gapMapCacheService.find(courseId)).thenReturn(Optional.empty());
        when(subtopicMasteryRepository.findAllByCourseId(courseId)).thenReturn(List.of());

        var map = service.handle(new GetMasteryGapMapByCourseQuery(TEACHER, courseId));

        assertThat(map.hasSufficientData()).isFalse();
        assertThat(map.subtopics()).hasSize(2).allSatisfy(line -> assertThat(line.noDataCount()).isEqualTo(2));
        assertThat(map.students()).hasSize(4).allSatisfy(cell -> assertThat(cell.level()).isEqualTo(MasteryLevel.NO_DATA));
    }

    @Test
    void shouldServeTheCachedGapMapWhileItCoversTheRoster() {
        var cell = new StudentMasteryCell(ana.value(), "ana@kalibra.pe", equations.value(), Optional.empty(), MasteryLevel.NO_DATA);
        var other = new StudentMasteryCell(luis.value(), "luis@kalibra.pe", equations.value(), Optional.empty(), MasteryLevel.NO_DATA);
        var cached = new MasteryGapMap(courseId.value(), false, List.of(), List.of(cell, other));
        when(gapMapCacheService.find(courseId)).thenReturn(Optional.of(cached));

        assertThat(service.handle(new GetMasteryGapMapByCourseQuery(TEACHER, courseId))).isSameAs(cached);
        verifyNoInteractions(subtopicMasteryRepository);
        verify(gapMapCacheService, never()).store(any());
    }

    @Test
    void shouldRebuildTheGapMapWhenTheCachedOneMissesANewlyEnrolledStudent() {
        var cell = new StudentMasteryCell(ana.value(), "ana@kalibra.pe", equations.value(), Optional.empty(), MasteryLevel.NO_DATA);
        var stale = new MasteryGapMap(courseId.value(), false, List.of(), List.of(cell));
        when(gapMapCacheService.find(courseId)).thenReturn(Optional.of(stale));
        when(subtopicMasteryRepository.findAllByCourseId(courseId)).thenReturn(List.of());

        var map = service.handle(new GetMasteryGapMapByCourseQuery(TEACHER, courseId));

        assertThat(map).isNotSameAs(stale);
        assertThat(map.students()).extracting(StudentMasteryCell::studentId).contains(luis.value());
        verify(gapMapCacheService).store(map);
    }

    @Test
    void shouldHideTheGapMapOfAnotherTeachersCourse() {
        assertThatThrownBy(() -> service.handle(new GetMasteryGapMapByCourseQuery("teacher-2", courseId)))
                .isInstanceOf(CourseNotOwnedByTeacherException.class);
        verifyNoInteractions(gapMapCacheService, subtopicMasteryRepository);
    }

    @Test
    void shouldComposeTheFourIndicatorsOfTheCourse() {
        // Arrange
        when(exerciseAttemptRepository.findAllByCourseId(courseId)).thenReturn(List.of(
                ProgressFixtures.attempt(anaHolder, courseId, equations, "B", 0.30, 0.52),
                ProgressFixtures.attempt(anaHolder, courseId, equations, "A", 0.52, 0.44),
                ProgressFixtures.attempt(anaHolder, courseId, equations, "B", 0.44, 0.60)));
        when(subtopicMasteryRepository.findAllByCourseId(courseId)).thenReturn(List.of(
                ProgressFixtures.mastery(anaHolder, courseId, equations, 0.52, 0.60, MasteryLevel.MEDIUM)));
        when(externalCurriculumService.fetchVerificationStats(courseId)).thenReturn(List.of(
                new SubtopicVerificationStats(equations.value(), "Equations", 8, 6)));

        // Act
        var report = service.handle(new GetCourseIndicatorsQuery(TEACHER, courseId));

        // Assert
        assertThat(report.courseId()).isEqualTo(courseId.value());
        assertThat(report.hasActivity()).isTrue();

        assertThat(report.accuracy().groupAccuracy()).isEqualTo(66.7);
        assertThat(report.accuracy().perStudent()).hasSize(2);
        var active = report.accuracy().perStudent().getFirst();
        assertThat(active.email()).isEqualTo("ana@kalibra.pe");
        assertThat(active.correct()).isEqualTo(2);
        assertThat(active.submitted()).isEqualTo(3);
        assertThat(active.accuracy()).contains(66.7);
        var inactive = report.accuracy().perStudent().get(1);
        assertThat(inactive.email()).isEqualTo("luis@kalibra.pe");
        assertThat(inactive.hasActivity()).isFalse();
        assertThat(inactive.accuracy()).isEmpty();

        assertThat(report.practice().totalSolved()).isEqualTo(3);
        assertThat(report.practice().activeStudents()).isEqualTo(1);
        assertThat(report.practice().enrolledStudents()).isEqualTo(2);
        assertThat(report.practice().averagePerActiveStudent()).isEqualTo(3.0);
        assertThat(report.practice().perSubtopic()).extracting("subtopicName", "solved")
                .containsExactly(org.assertj.core.groups.Tuple.tuple("Equations", 3),
                        org.assertj.core.groups.Tuple.tuple("Inequalities", 0));

        var practiced = report.evolution().perSubtopic().getFirst();
        assertThat(practiced.subtopicName()).isEqualTo("Equations");
        assertThat(practiced.initialAverage()).contains(52.0);
        assertThat(practiced.currentAverage()).contains(60.0);
        assertThat(practiced.deltaPoints()).contains(8);
        assertThat(practiced.level()).isEqualTo(MasteryLevel.MEDIUM);
        var unpracticed = report.evolution().perSubtopic().get(1);
        assertThat(unpracticed.practiced()).isFalse();
        assertThat(unpracticed.deltaPoints()).isEmpty();
        assertThat(unpracticed.initialAverage()).isEmpty();
        assertThat(report.evolution().groupDeltaPoints()).isEqualTo(8);

        assertThat(report.verification().approvalRate()).isEqualTo(75.0);
        assertThat(report.verification().approved()).isEqualTo(6);
        assertThat(report.verification().discarded()).isEqualTo(2);
        assertThat(report.verification().perSubtopic()).singleElement()
                .satisfies(line -> {
                    assertThat(line.subtopicName()).isEqualTo("Equations");
                    assertThat(line.approvalRate()).isEqualTo(75.0);
                });
    }

    @Test
    void shouldReportNoActivityInsteadOfZeroedIndicatorsForACourseWithoutSolvedExercises() {
        when(exerciseAttemptRepository.findAllByCourseId(courseId)).thenReturn(List.of());
        when(subtopicMasteryRepository.findAllByCourseId(courseId)).thenReturn(List.of());
        when(externalCurriculumService.fetchVerificationStats(courseId)).thenReturn(List.of());

        var report = service.handle(new GetCourseIndicatorsQuery(TEACHER, courseId));

        assertThat(report.hasActivity()).isFalse();
        assertThat(report.accuracy().perStudent()).hasSize(2).allSatisfy(line -> assertThat(line.hasActivity()).isFalse());
        assertThat(report.evolution().perSubtopic()).allSatisfy(line -> assertThat(line.practiced()).isFalse());
        assertThat(report.verification().perSubtopic()).isEmpty();
    }

    @Test
    void shouldHideTheIndicatorsOfAnotherTeachersCourse() {
        assertThatThrownBy(() -> service.handle(new GetCourseIndicatorsQuery("teacher-2", courseId)))
                .isInstanceOf(CourseNotOwnedByTeacherException.class);
        assertThatThrownBy(() -> service.handle(new ExportCourseIndicatorsQuery("teacher-2", courseId)))
                .isInstanceOf(CourseNotOwnedByTeacherException.class);
        verifyNoInteractions(exerciseAttemptRepository, subtopicMasteryRepository);
    }

    @Test
    void shouldExportOneRowPerStudentAndSubtopicIdentifiedOnlyByAnAnonymousCode() {
        // Arrange
        when(exerciseAttemptRepository.findAllByCourseId(courseId)).thenReturn(List.of(
                ProgressFixtures.attempt(anaHolder, courseId, equations, "B", 0.30, 0.52),
                ProgressFixtures.attempt(anaHolder, courseId, equations, "A", 0.52, 0.44),
                ProgressFixtures.attempt(anaHolder, courseId, inequalities, "B", 0.30, 0.50),
                ProgressFixtures.attempt(luis.value().toString(), courseId, equations, "A", 0.30, 0.20)));
        when(subtopicMasteryRepository.findAllByCourseId(courseId)).thenReturn(List.of(
                ProgressFixtures.mastery(anaHolder, courseId, equations, 0.52, 0.44, MasteryLevel.MEDIUM)));
        when(externalCurriculumService.fetchVerificationStats(courseId)).thenReturn(List.of(
                new SubtopicVerificationStats(equations.value(), "Equations", 8, 6)));

        // Act
        var export = service.handle(new ExportCourseIndicatorsQuery(TEACHER, courseId));

        // Assert
        assertThat(export.fileName()).endsWith(".csv");
        assertThat(export.rows()).hasSize(3);
        var anaCode = new StudentAnonymizer().codeFor(ana);
        var anaEquations = export.rows().stream()
                .filter(row -> row.studentCode().equals(anaCode) && row.subtopicName().equals("Equations"))
                .findFirst().orElseThrow();
        assertThat(anaEquations.solved()).isEqualTo(2);
        assertThat(anaEquations.accuracy()).isEqualTo(50.0);
        assertThat(anaEquations.initialMastery()).isEqualTo(52.0);
        assertThat(anaEquations.currentMastery()).isEqualTo(44.0);
        assertThat(anaEquations.verificationApprovalRate()).isEqualTo(75.0);
        var anaInequalities = export.rows().stream()
                .filter(row -> row.studentCode().equals(anaCode) && row.subtopicName().equals("Inequalities"))
                .findFirst().orElseThrow();
        assertThat(anaInequalities.currentMastery()).isEqualTo(50.0);
        assertThat(anaInequalities.verificationApprovalRate()).isZero();
        assertThat(export.rows()).allSatisfy(row -> assertThat(row.toString())
                .doesNotContain("@kalibra.pe", ana.value().toString(), luis.value().toString()));
    }

    @Test
    void shouldNotOfferTheExportForACourseWithoutIndicators() {
        when(exerciseAttemptRepository.findAllByCourseId(courseId)).thenReturn(List.of());

        assertThatThrownBy(() -> service.handle(new ExportCourseIndicatorsQuery(TEACHER, courseId)))
                .isInstanceOf(NoIndicatorsAvailableException.class);
    }

    @Test
    void shouldAnswerTheGuideOfTheFourIndicators() {
        assertThat(service.handle(new GetIndicatorGuideQuery()).entries()).hasSize(4);
    }
}
