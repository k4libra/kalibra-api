package com.kalibra.api.progress.application.internal.commandservices;

import com.kalibra.api.progress.ProgressFixtures;
import com.kalibra.api.progress.application.internal.outboundservices.acl.ExternalCurriculumService;
import com.kalibra.api.progress.application.internal.outboundservices.acl.ExternalEnrollmentService;
import com.kalibra.api.progress.application.internal.outboundservices.acl.ExternalMasteryEstimationService;
import com.kalibra.api.progress.domain.exceptions.ExerciseNotFoundException;
import com.kalibra.api.progress.domain.exceptions.NotEnrolledInCourseException;
import com.kalibra.api.progress.domain.model.aggregates.ExerciseAttempt;
import com.kalibra.api.progress.domain.model.commands.RequestPracticeExerciseCommand;
import com.kalibra.api.progress.domain.model.commands.SubmitAnswerCommand;
import com.kalibra.api.progress.domain.model.events.AnswerRecorded;
import com.kalibra.api.progress.domain.model.valueobjects.AnswerResult;
import com.kalibra.api.progress.domain.model.valueobjects.CourseId;
import com.kalibra.api.progress.domain.model.valueobjects.ExerciseId;
import com.kalibra.api.progress.domain.model.valueobjects.MasteryEstimate;
import com.kalibra.api.progress.domain.model.valueobjects.MasteryLevel;
import com.kalibra.api.progress.domain.model.valueobjects.MasteryProbability;
import com.kalibra.api.progress.domain.model.valueobjects.PracticeExerciseView;
import com.kalibra.api.progress.domain.model.valueobjects.StudentId;
import com.kalibra.api.progress.domain.model.valueobjects.SubtopicId;
import com.kalibra.api.progress.domain.repositories.ExerciseAttemptRepository;
import com.kalibra.api.progress.domain.repositories.SubtopicMasteryRepository;
import com.kalibra.api.shared.contracts.curriculum.SubtopicSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExerciseAttemptCommandServiceImplTest {

    @Mock
    ExerciseAttemptRepository exerciseAttemptRepository;

    @Mock
    SubtopicMasteryRepository subtopicMasteryRepository;

    @Mock
    ExternalCurriculumService externalCurriculumService;

    @Mock
    ExternalEnrollmentService externalEnrollmentService;

    @Mock
    ExternalMasteryEstimationService externalMasteryEstimationService;

    @Mock
    ApplicationEventPublisher eventPublisher;

    @Mock
    PlatformTransactionManager transactionManager;

    ExerciseAttemptCommandServiceImpl service;

    private final StudentId studentId = new StudentId(UUID.randomUUID());
    private final String holderId = studentId.value().toString();
    private final CourseId courseId = new CourseId(UUID.randomUUID());
    private final SubtopicId subtopicId = new SubtopicId(UUID.randomUUID());
    private final ExerciseId exerciseId = new ExerciseId(UUID.randomUUID());
    private final SubmitAnswerCommand correctAnswer = new SubmitAnswerCommand(holderId, courseId, exerciseId, "B");

    @BeforeEach
    void setUp() {
        service = new ExerciseAttemptCommandServiceImpl(exerciseAttemptRepository, subtopicMasteryRepository,
                externalCurriculumService, externalEnrollmentService, externalMasteryEstimationService,
                eventPublisher, transactionManager);
    }

    private void givenAnEnrolledStudentAndAnExerciseOfTheCourse() {
        when(externalEnrollmentService.isEnrolled(studentId, courseId)).thenReturn(true);
        when(externalCurriculumService.fetchAnswerKey(exerciseId)).thenReturn(Optional.of(ProgressFixtures.answerKey(subtopicId)));
        when(externalCurriculumService.fetchSubtopics(courseId))
                .thenReturn(List.of(new SubtopicSummary(subtopicId.value(), "Equations", 1)));
        when(exerciseAttemptRepository.save(any(ExerciseAttempt.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void shouldRecordTheAnswerWithItsMasteryChangeAndPublishIt() {
        // Arrange
        givenAnEnrolledStudentAndAnExerciseOfTheCourse();
        var previous = ProgressFixtures.attempt(holderId, courseId, subtopicId, "A", 0.30, 0.41);
        var event = ArgumentCaptor.forClass(AnswerRecorded.class);
        when(exerciseAttemptRepository.findFirstByHolderIdAndSubtopicIdOrderByAnsweredAtDesc(holderId, subtopicId))
                .thenReturn(Optional.of(previous));
        when(externalMasteryEstimationService.estimate(holderId, subtopicId, Optional.of(new MasteryProbability(0.41)), AnswerResult.CORRECT))
                .thenReturn(new MasteryEstimate(new MasteryProbability(0.63), MasteryLevel.MEDIUM));

        // Act
        var attempt = service.handle(correctAnswer);

        // Assert
        assertThat(attempt.isCorrect()).isTrue();
        assertThat(attempt.getFeedback().explanation()).isEqualTo(ProgressFixtures.EXPLANATION);
        assertThat(attempt.getMasteryChange().previous()).contains(new MasteryProbability(0.41));
        assertThat(attempt.getMasteryChange().current()).isEqualTo(new MasteryProbability(0.63));
        assertThat(attempt.getMasteryChange().deltaPoints()).isEqualTo(22);
        verify(eventPublisher).publishEvent(event.capture());
        assertThat(event.getValue().attemptId()).isEqualTo(attempt.getId().value());
        assertThat(event.getValue().holderId()).isEqualTo(holderId);
        assertThat(event.getValue().courseId()).isEqualTo(courseId.value());
        assertThat(event.getValue().subtopicId()).isEqualTo(subtopicId.value());
        assertThat(event.getValue().probability()).isEqualTo(0.63);
        assertThat(event.getValue().level()).isEqualTo("MEDIUM");
    }

    @Test
    void shouldStartFromTheBaseOfTheEngineOnTheFirstAnswerOfASubtopic() {
        // Arrange
        givenAnEnrolledStudentAndAnExerciseOfTheCourse();
        when(exerciseAttemptRepository.findFirstByHolderIdAndSubtopicIdOrderByAnsweredAtDesc(holderId, subtopicId))
                .thenReturn(Optional.empty());
        when(subtopicMasteryRepository.findByHolderIdAndSubtopicId(holderId, subtopicId)).thenReturn(Optional.empty());
        when(externalMasteryEstimationService.estimate(holderId, subtopicId, Optional.empty(), AnswerResult.CORRECT))
                .thenReturn(new MasteryEstimate(new MasteryProbability(0.52), MasteryLevel.MEDIUM,
                        Optional.of(new MasteryProbability(0.30)), true));

        // Act
        var attempt = service.handle(correctAnswer);

        // Assert
        assertThat(attempt.getMasteryChange().previous()).contains(new MasteryProbability(0.30));
        assertThat(attempt.getMasteryChange().deltaPoints()).isEqualTo(22);
        assertThat(attempt.getMasteryChange().hasChanged()).isTrue();
    }

    @Test
    void shouldUseTheStoredMasteryWhenThereIsNoPreviousAttempt() {
        givenAnEnrolledStudentAndAnExerciseOfTheCourse();
        var wrongAnswer = new SubmitAnswerCommand(holderId, courseId, exerciseId, "D");
        when(exerciseAttemptRepository.findFirstByHolderIdAndSubtopicIdOrderByAnsweredAtDesc(holderId, subtopicId))
                .thenReturn(Optional.empty());
        when(subtopicMasteryRepository.findByHolderIdAndSubtopicId(holderId, subtopicId))
                .thenReturn(Optional.of(ProgressFixtures.mastery(holderId, courseId, subtopicId, 0.30, 0.55, MasteryLevel.MEDIUM)));
        when(externalMasteryEstimationService.estimate(holderId, subtopicId, Optional.of(new MasteryProbability(0.55)), AnswerResult.INCORRECT))
                .thenReturn(new MasteryEstimate(new MasteryProbability(0.47), MasteryLevel.MEDIUM));

        var attempt = service.handle(wrongAnswer);

        assertThat(attempt.isCorrect()).isFalse();
        assertThat(attempt.getMasteryChange().deltaPoints()).isEqualTo(-8);
    }

    @Test
    void shouldRejectAnAnswerFromAStudentWhoIsNotEnrolled() {
        when(externalEnrollmentService.isEnrolled(studentId, courseId)).thenReturn(false);

        assertThatThrownBy(() -> service.handle(correctAnswer)).isInstanceOf(NotEnrolledInCourseException.class);
        verifyNoInteractions(externalCurriculumService, externalMasteryEstimationService, eventPublisher);
        verify(exerciseAttemptRepository, never()).save(any());
    }

    @Test
    void shouldRejectAnAnswerToAnUnknownOrDiscardedExercise() {
        when(externalEnrollmentService.isEnrolled(studentId, courseId)).thenReturn(true);
        when(externalCurriculumService.fetchAnswerKey(exerciseId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.handle(correctAnswer)).isInstanceOf(ExerciseNotFoundException.class);
        verifyNoInteractions(externalMasteryEstimationService, eventPublisher);
        verify(exerciseAttemptRepository, never()).save(any());
    }

    @Test
    void shouldRejectAnAnswerToAnExerciseOfAnotherCourse() {
        when(externalEnrollmentService.isEnrolled(studentId, courseId)).thenReturn(true);
        when(externalCurriculumService.fetchAnswerKey(exerciseId)).thenReturn(Optional.of(ProgressFixtures.answerKey(subtopicId)));
        when(externalCurriculumService.fetchSubtopics(courseId))
                .thenReturn(List.of(new SubtopicSummary(UUID.randomUUID(), "Sorting", 1)));

        assertThatThrownBy(() -> service.handle(correctAnswer)).isInstanceOf(ExerciseNotFoundException.class);
        verifyNoInteractions(externalMasteryEstimationService, eventPublisher);
    }

    @Test
    void shouldRequestAnExerciseAdjustedToTheCurrentMastery() {
        // Arrange
        var command = new RequestPracticeExerciseCommand(holderId, courseId, subtopicId);
        var view = new PracticeExerciseView(exerciseId.value(), subtopicId.value(), "Solve 2x + 3 = 7",
                List.of("x = 1", "x = 2", "x = 3", "x = 5"));
        when(externalEnrollmentService.isEnrolled(studentId, courseId)).thenReturn(true);
        when(exerciseAttemptRepository.findFirstByHolderIdAndSubtopicIdOrderByAnsweredAtDesc(holderId, subtopicId))
                .thenReturn(Optional.of(ProgressFixtures.attempt(holderId, courseId, subtopicId, "B", 0.30, 0.52)));
        when(externalCurriculumService.provideExercise(command, Optional.of(new MasteryProbability(0.52))))
                .thenReturn(Optional.of(view));

        // Act & Assert
        assertThat(service.handle(command)).contains(view);
    }

    @Test
    void shouldRequestAnExerciseWithoutMasteryForAFirstPractice() {
        var command = new RequestPracticeExerciseCommand(holderId, courseId, subtopicId);
        when(externalEnrollmentService.isEnrolled(studentId, courseId)).thenReturn(true);
        when(exerciseAttemptRepository.findFirstByHolderIdAndSubtopicIdOrderByAnsweredAtDesc(holderId, subtopicId))
                .thenReturn(Optional.empty());
        when(subtopicMasteryRepository.findByHolderIdAndSubtopicId(holderId, subtopicId)).thenReturn(Optional.empty());
        when(externalCurriculumService.provideExercise(command, Optional.empty())).thenReturn(Optional.empty());

        assertThat(service.handle(command)).isEmpty();
    }

    @Test
    void shouldRejectAnExerciseRequestFromAStudentWhoIsNotEnrolledOrFromANonStudentSubject() {
        var command = new RequestPracticeExerciseCommand(holderId, courseId, subtopicId);
        var notAStudent = new RequestPracticeExerciseCommand("not-a-uuid", courseId, subtopicId);
        when(externalEnrollmentService.isEnrolled(studentId, courseId)).thenReturn(false);

        assertThatThrownBy(() -> service.handle(command)).isInstanceOf(NotEnrolledInCourseException.class);
        assertThatThrownBy(() -> service.handle(notAStudent)).isInstanceOf(NotEnrolledInCourseException.class);
        verifyNoInteractions(externalCurriculumService);
    }
}
