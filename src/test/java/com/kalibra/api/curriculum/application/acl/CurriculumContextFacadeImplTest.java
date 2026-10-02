package com.kalibra.api.curriculum.application.acl;

import com.kalibra.api.curriculum.GeneratedExerciseFixtures;
import com.kalibra.api.curriculum.domain.exceptions.SubtopicWithoutIngestedMaterialException;
import com.kalibra.api.curriculum.domain.model.aggregates.Course;
import com.kalibra.api.curriculum.domain.model.commands.CreateCourseCommand;
import com.kalibra.api.curriculum.domain.model.commands.GenerateExerciseForStudentCommand;
import com.kalibra.api.curriculum.domain.model.queries.GetCourseByIdQuery;
import com.kalibra.api.curriculum.domain.model.queries.GetCoursesByHolderIdQuery;
import com.kalibra.api.curriculum.domain.model.queries.GetGeneratedExerciseByIdQuery;
import com.kalibra.api.curriculum.domain.model.queries.GetVerificationApprovalByCourseQuery;
import com.kalibra.api.curriculum.domain.model.valueobjects.CourseCode;
import com.kalibra.api.curriculum.domain.model.valueobjects.MasteryProbability;
import com.kalibra.api.curriculum.domain.model.valueobjects.SubtopicExerciseCount;
import com.kalibra.api.curriculum.domain.model.valueobjects.SubtopicId;
import com.kalibra.api.curriculum.domain.model.valueobjects.VerificationApprovalReport;
import com.kalibra.api.curriculum.domain.services.CourseQueryService;
import com.kalibra.api.curriculum.domain.services.GeneratedExerciseCommandService;
import com.kalibra.api.curriculum.domain.services.GeneratedExerciseQueryService;
import com.kalibra.api.shared.contracts.curriculum.PracticeExerciseRequest;
import com.kalibra.api.shared.contracts.curriculum.SubtopicVerificationStats;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurriculumContextFacadeImplTest {

    @Mock
    CourseQueryService courseQueryService;

    @Mock
    GeneratedExerciseCommandService generatedExerciseCommandService;

    @Mock
    GeneratedExerciseQueryService generatedExerciseQueryService;

    @InjectMocks
    CurriculumContextFacadeImpl facade;

    private final Course course = Course.create(new CreateCourseCommand("teacher-1", "Algebra",
            new CourseCode("MAT101"), List.of("Equations", "Inequalities")));
    private final SubtopicId subtopicId = course.getSubtopics().getFirst().getId();

    @Test
    void shouldTellWhetherTheCourseBelongsToTheTeacher() {
        when(courseQueryService.handle(any(GetCourseByIdQuery.class))).thenReturn(Optional.of(course));

        assertThat(facade.isCourseOwnedBy(course.getId().value(), "teacher-1")).isTrue();
        assertThat(facade.isCourseOwnedBy(course.getId().value(), "teacher-2")).isFalse();
    }

    @Test
    void shouldAnswerFalseAndEmptyForAnUnknownCourse() {
        when(courseQueryService.handle(any(GetCourseByIdQuery.class))).thenReturn(Optional.empty());
        var unknown = UUID.randomUUID();

        assertThat(facade.isCourseOwnedBy(unknown, "teacher-1")).isFalse();
        assertThat(facade.fetchCourseSummary(unknown)).isEmpty();
        assertThat(facade.fetchSubtopics(unknown)).isEmpty();
    }

    @Test
    void shouldExposeSummariesWithNeutralTypes() {
        // Arrange
        when(courseQueryService.handle(any(GetCourseByIdQuery.class))).thenReturn(Optional.of(course));
        when(courseQueryService.handle(any(GetCoursesByHolderIdQuery.class))).thenReturn(List.of(course));

        // Act
        var summary = facade.fetchCourseSummary(course.getId().value());
        var subtopics = facade.fetchSubtopics(course.getId().value());

        // Assert
        assertThat(summary).isPresent();
        assertThat(summary.get().code()).isEqualTo("MAT101");
        assertThat(summary.get().holderId()).isEqualTo("teacher-1");
        assertThat(facade.fetchCoursesByHolderId("teacher-1")).containsExactly(summary.get());
        assertThat(subtopics).extracting("name").containsExactly("Equations", "Inequalities");
        assertThat(subtopics).extracting("displayOrder").containsExactly(1, 2);
    }

    @Test
    void shouldProvideAnApprovedExerciseWithoutItsAnswer() {
        // Arrange
        var approved = GeneratedExerciseFixtures.approved(course, subtopicId);
        var command = ArgumentCaptor.forClass(GenerateExerciseForStudentCommand.class);
        when(generatedExerciseCommandService.handle(any(GenerateExerciseForStudentCommand.class))).thenReturn(Optional.of(approved));

        // Act
        var snapshot = facade.provideExerciseForStudent(
                new PracticeExerciseRequest(course.getId().value(), subtopicId.value(), Optional.of(0.4)));

        // Assert
        assertThat(snapshot).isPresent();
        assertThat(snapshot.get().exerciseId()).isEqualTo(approved.getId().value());
        assertThat(snapshot.get().statement()).isEqualTo("Solve 2x + 3 = 7");
        assertThat(snapshot.get().options()).containsExactly("x = 1", "x = 2", "x = 3", "x = 5");
        verify(generatedExerciseCommandService).handle(command.capture());
        assertThat(command.getValue().mastery()).contains(new MasteryProbability(0.4));
        assertThat(command.getValue().subtopicId()).isEqualTo(subtopicId);
    }

    @Test
    void shouldNeverProvideADiscardedExerciseToAStudent() {
        var discarded = GeneratedExerciseFixtures.discarded(course, subtopicId);
        when(generatedExerciseCommandService.handle(any(GenerateExerciseForStudentCommand.class))).thenReturn(Optional.of(discarded));

        assertThat(facade.provideExerciseForStudent(
                new PracticeExerciseRequest(course.getId().value(), subtopicId.value(), Optional.empty()))).isEmpty();
    }

    @Test
    void shouldProvideNothingWhenTheSubtopicHasNoIngestedMaterial() {
        when(generatedExerciseCommandService.handle(any(GenerateExerciseForStudentCommand.class)))
                .thenThrow(new SubtopicWithoutIngestedMaterialException(subtopicId.value()));

        assertThat(facade.provideExerciseForStudent(
                new PracticeExerciseRequest(course.getId().value(), subtopicId.value(), Optional.empty()))).isEmpty();
    }

    @Test
    void shouldExposeTheAnswerKeyOfAnApprovedExerciseOnly() {
        // Arrange
        var approved = GeneratedExerciseFixtures.approved(course, subtopicId);
        var discarded = GeneratedExerciseFixtures.discarded(course, subtopicId);
        when(generatedExerciseQueryService.handle(new GetGeneratedExerciseByIdQuery(approved.getId()))).thenReturn(Optional.of(approved));
        when(generatedExerciseQueryService.handle(new GetGeneratedExerciseByIdQuery(discarded.getId()))).thenReturn(Optional.of(discarded));

        // Act
        var key = facade.fetchAnswerKey(approved.getId().value());

        // Assert
        assertThat(key).isPresent();
        assertThat(key.get().correctOptionKey()).isEqualTo("B");
        assertThat(key.get().subtopicId()).isEqualTo(subtopicId.value());
        assertThat(key.get().explanation()).isEqualTo("Subtract 3, divide by 2");
        assertThat(facade.fetchAnswerKey(discarded.getId().value())).isEmpty();
        assertThat(facade.fetchAnswerKey(null)).isEmpty();
    }

    @Test
    void shouldExposeTheVerificationStatsPerSubtopic() {
        var report = new VerificationApprovalReport(4, 3, 1, 75.0,
                List.of(new SubtopicExerciseCount(subtopicId.value(), "Equations", 4, 3, 1)));
        when(generatedExerciseQueryService.handle(new GetVerificationApprovalByCourseQuery(course.getId()))).thenReturn(report);

        assertThat(facade.fetchVerificationStats(course.getId().value()))
                .containsExactly(new SubtopicVerificationStats(subtopicId.value(), "Equations", 4, 3));
        assertThat(facade.fetchVerificationStats(null)).isEmpty();
    }
}
