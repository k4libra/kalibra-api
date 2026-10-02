package com.kalibra.api.progress.application.internal.outboundservices.acl;

import com.kalibra.api.curriculum.interfaces.acl.CurriculumContextFacade;
import com.kalibra.api.enrollment.interfaces.acl.EnrollmentContextFacade;
import com.kalibra.api.progress.domain.model.commands.RequestPracticeExerciseCommand;
import com.kalibra.api.progress.domain.model.valueobjects.CourseId;
import com.kalibra.api.progress.domain.model.valueobjects.ExerciseId;
import com.kalibra.api.progress.domain.model.valueobjects.MasteryProbability;
import com.kalibra.api.progress.domain.model.valueobjects.StudentId;
import com.kalibra.api.progress.domain.model.valueobjects.SubtopicId;
import com.kalibra.api.shared.contracts.curriculum.ExerciseAnswerKey;
import com.kalibra.api.shared.contracts.curriculum.ExerciseSnapshot;
import com.kalibra.api.shared.contracts.curriculum.PracticeExerciseRequest;
import com.kalibra.api.shared.contracts.enrollment.EnrollmentLookupRequest;
import com.kalibra.api.shared.contracts.enrollment.RosterEntry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProgressOutboundServicesTest {

    @Mock
    CurriculumContextFacade curriculumContextFacade;

    @Mock
    EnrollmentContextFacade enrollmentContextFacade;

    private final CourseId courseId = new CourseId(UUID.randomUUID());
    private final SubtopicId subtopicId = new SubtopicId(UUID.randomUUID());
    private final ExerciseId exerciseId = new ExerciseId(UUID.randomUUID());
    private final StudentId studentId = new StudentId(UUID.randomUUID());

    @Test
    void shouldTranslateTheAnswerKeyOfCurriculumIntoTheProgressModel() {
        var service = new ExternalCurriculumService(curriculumContextFacade);
        when(curriculumContextFacade.fetchAnswerKey(exerciseId.value())).thenReturn(Optional.of(
                new ExerciseAnswerKey(exerciseId.value(), subtopicId.value(), "Solve 2x + 3 = 7", "B", "Subtract 3")));

        var key = service.fetchAnswerKey(exerciseId);

        assertThat(key).isPresent();
        assertThat(key.get().subtopicId()).isEqualTo(subtopicId);
        assertThat(key.get().isCorrect("B")).isTrue();
        assertThat(key.get().explanation()).isEqualTo("Subtract 3");
    }

    @Test
    void shouldAskCurriculumForAnExerciseWithTheCurrentMastery() {
        var service = new ExternalCurriculumService(curriculumContextFacade);
        var command = new RequestPracticeExerciseCommand(studentId.value().toString(), courseId, subtopicId);
        when(curriculumContextFacade.provideExerciseForStudent(
                new PracticeExerciseRequest(courseId.value(), subtopicId.value(), Optional.of(0.52))))
                .thenReturn(Optional.of(new ExerciseSnapshot(exerciseId.value(), subtopicId.value(), "Solve 2x + 3 = 7",
                        List.of("x = 1", "x = 2", "x = 3", "x = 5"))));

        var view = service.provideExercise(command, Optional.of(new MasteryProbability(0.52)));

        assertThat(view).isPresent();
        assertThat(view.get().exerciseId()).isEqualTo(exerciseId.value());
        assertThat(view.get().options()).hasSize(4);
    }

    @Test
    void shouldDelegateOwnershipToCurriculum() {
        var service = new ExternalCurriculumService(curriculumContextFacade);
        when(curriculumContextFacade.isCourseOwnedBy(courseId.value(), "teacher-1")).thenReturn(true);

        assertThat(service.isCourseOwnedBy(courseId, "teacher-1")).isTrue();
        assertThat(service.isCourseOwnedBy(courseId, "teacher-2")).isFalse();
    }

    @Test
    void shouldAskEnrollmentForTheEnrollmentAndTheRoster() {
        var service = new ExternalEnrollmentService(enrollmentContextFacade);
        when(enrollmentContextFacade.isStudentEnrolled(new EnrollmentLookupRequest(studentId.value(), courseId.value())))
                .thenReturn(true);
        when(enrollmentContextFacade.fetchCourseRoster(courseId.value()))
                .thenReturn(List.of(new RosterEntry(studentId.value(), "ana@kalibra.pe", Instant.now())));

        assertThat(service.isEnrolled(studentId, courseId)).isTrue();
        assertThat(service.fetchRoster(courseId)).containsExactly(studentId);
        assertThat(service.fetchRosterEntries(courseId)).extracting(RosterEntry::email).containsExactly("ana@kalibra.pe");
    }
}
