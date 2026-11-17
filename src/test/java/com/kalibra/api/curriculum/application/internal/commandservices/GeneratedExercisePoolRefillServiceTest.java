package com.kalibra.api.curriculum.application.internal.commandservices;

import com.kalibra.api.curriculum.domain.model.commands.GenerateExerciseForStudentCommand;
import com.kalibra.api.curriculum.domain.model.valueobjects.CourseId;
import com.kalibra.api.curriculum.domain.model.valueobjects.CurricularContent;
import com.kalibra.api.curriculum.domain.model.valueobjects.DifficultyLevel;
import com.kalibra.api.curriculum.domain.model.valueobjects.ExerciseId;
import com.kalibra.api.curriculum.domain.model.valueobjects.MasteryProbability;
import com.kalibra.api.curriculum.domain.model.valueobjects.SubtopicId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.TaskRejectedException;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GeneratedExercisePoolRefillServiceTest {

    @Mock
    GeneratedExercisePoolRefillTask task;

    @InjectMocks
    GeneratedExercisePoolRefillService service;

    private final GenerateExerciseForStudentCommand command = new GenerateExerciseForStudentCommand(
            new CourseId(UUID.randomUUID()), new SubtopicId(UUID.randomUUID()), Optional.empty(), Set.of(), Optional.of(DifficultyLevel.EASY));
    private final CurricularContent context = new CurricularContent("Linear equations", 1);

    @Test
    void shouldDeduplicateRefillsWhenTheKeyIsInFlight() {
        var pending = new CompletableFuture<Void>();
        when(task.refill(command, context)).thenReturn(pending, CompletableFuture.completedFuture(null));

        service.request(command, context);
        service.request(command, context);
        verify(task).refill(command, context);
        pending.complete(null);
        service.request(command, context);
        verify(task, times(2)).refill(command, context);
    }

    @Test
    void shouldDeduplicateWhenOnlyMasteryOrExclusionsDiffer() {
        var equivalent = new GenerateExerciseForStudentCommand(command.courseId(), command.subtopicId(),
                Optional.of(new MasteryProbability(0.2)),
                Set.of(new ExerciseId(UUID.randomUUID())), command.targetDifficulty());
        var otherDifficulty = new GenerateExerciseForStudentCommand(command.courseId(), command.subtopicId(),
                command.mastery(), Set.of(), Optional.of(DifficultyLevel.MEDIUM));
        var otherSubtopic = new GenerateExerciseForStudentCommand(command.courseId(), new SubtopicId(UUID.randomUUID()),
                command.mastery(), Set.of(), command.targetDifficulty());
        var otherCourse = new GenerateExerciseForStudentCommand(new CourseId(UUID.randomUUID()), command.subtopicId(),
                command.mastery(), Set.of(), command.targetDifficulty());
        when(task.refill(command, context)).thenReturn(new CompletableFuture<>());
        when(task.refill(otherDifficulty, context)).thenReturn(new CompletableFuture<>());
        when(task.refill(otherSubtopic, context)).thenReturn(new CompletableFuture<>());
        when(task.refill(otherCourse, context)).thenReturn(new CompletableFuture<>());

        service.request(command, context);
        service.request(equivalent, context);
        service.request(otherDifficulty, context);
        service.request(otherSubtopic, context);
        service.request(otherCourse, context);

        verify(task).refill(command, context);
        verify(task).refill(otherDifficulty, context);
        verify(task).refill(otherSubtopic, context);
        verify(task).refill(otherCourse, context);
    }

    @Test
    void shouldReleaseTheKeyWhenTheExecutorRejectsScheduling() {
        when(task.refill(command, context)).thenThrow(new TaskRejectedException("Queue full"))
                .thenReturn(CompletableFuture.completedFuture(null));

        assertThatCode(() -> service.request(command, context)).doesNotThrowAnyException();
        service.request(command, context);
        verify(task, times(2)).refill(command, context);
    }

    @Test
    void shouldReleaseTheKeyWhenTheRefillCompletesExceptionally() {
        var pending = new CompletableFuture<Void>();
        when(task.refill(command, context)).thenReturn(pending, CompletableFuture.completedFuture(null));
        service.request(command, context);

        pending.completeExceptionally(new IllegalStateException("Worker failed"));
        service.request(command, context);

        verify(task, times(2)).refill(command, context);
    }
}
