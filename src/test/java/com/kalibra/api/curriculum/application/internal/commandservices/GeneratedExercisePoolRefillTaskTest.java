package com.kalibra.api.curriculum.application.internal.commandservices;

import com.kalibra.api.curriculum.GeneratedExerciseFixtures;
import com.kalibra.api.curriculum.application.internal.outboundservices.acl.ExternalExerciseGenerationService;
import com.kalibra.api.curriculum.domain.model.aggregates.Course;
import com.kalibra.api.curriculum.domain.model.commands.CreateCourseCommand;
import com.kalibra.api.curriculum.domain.model.commands.GenerateExerciseForStudentCommand;
import com.kalibra.api.curriculum.domain.model.valueobjects.CourseCode;
import com.kalibra.api.curriculum.domain.model.valueobjects.CurricularContent;
import com.kalibra.api.curriculum.domain.model.valueobjects.DifficultyLevel;
import com.kalibra.api.curriculum.domain.repositories.GeneratedExerciseRepository;
import com.kalibra.api.shared.config.AsyncConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
class GeneratedExercisePoolRefillTaskTest {

    @Mock
    ExternalExerciseGenerationService generationService;

    @Mock
    GeneratedExerciseRepository repository;

    @InjectMocks
    GeneratedExercisePoolRefillTask task;

    private final Course course = Course.create(new CreateCourseCommand("teacher-1", "Algebra",
            new CourseCode("MAT101"), List.of("Equations")));
    private final GenerateExerciseForStudentCommand command = new GenerateExerciseForStudentCommand(
            course.getId(), course.getSubtopics().getFirst().getId(), Optional.empty(), Set.of(), Optional.of(DifficultyLevel.EASY));
    private final CurricularContent content = new CurricularContent("Linear equations", 1);

    @Test
    void shouldPersistEveryAttemptWhenTheEngineGenerates() {
        var attempts = List.of(GeneratedExerciseFixtures.discarded(course, command.subtopicId()),
                GeneratedExerciseFixtures.approved(course, command.subtopicId()));
        when(generationService.generate(command, content)).thenReturn(attempts);

        assertThat(task.refill(command, content)).isCompleted();
        verify(generationService).generate(command, content);
        verify(repository).saveAll(attempts);
    }

    @Test
    void shouldNotSaveWhenTheEngineFails() {
        when(generationService.generate(command, content)).thenThrow(new IllegalStateException("Engine unavailable"));

        assertThat(task.refill(command, content)).isCompleted();
        verifyNoInteractions(repository);
    }

    @Test
    void shouldNotThrowWhenPersistenceFails() {
        when(generationService.generate(command, content)).thenReturn(List.of());
        doThrow(new IllegalStateException("Database unavailable")).when(repository).saveAll(List.of());

        assertThat(task.refill(command, content)).isCompleted();
    }

    @Test
    void shouldRunOutsideTransactionsWhenScheduledOnTheRefillExecutor() throws Exception {
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var caller = Thread.currentThread();
        when(generationService.generate(command, content)).thenAnswer(call -> {
            assertThat(Thread.currentThread()).isNotSameAs(caller);
            assertThat(Thread.currentThread().getName()).startsWith("exercise-pool-refill-");
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            entered.countDown();
            assertThat(release.await(5, TimeUnit.SECONDS)).isTrue();
            return List.of();
        });

        try (var context = new AnnotationConfigApplicationContext()) {
            context.registerBean(ExternalExerciseGenerationService.class, () -> generationService);
            context.registerBean(GeneratedExerciseRepository.class, () -> repository);
            context.register(AsyncConfig.class, GeneratedExercisePoolRefillTask.class);
            context.refresh();
            try {
                var future = context.getBean(GeneratedExercisePoolRefillTask.class).refill(command, content);
                assertThat(entered.await(5, TimeUnit.SECONDS)).isTrue();
                assertThat(future).isNotDone();
                var executor = context.getBean("exercisePoolRefillExecutor", ThreadPoolTaskExecutor.class);
                assertThat(executor.getMaxPoolSize()).isEqualTo(2);
                assertThat(executor.getQueueCapacity()).isEqualTo(32);
                release.countDown();
                future.get(5, TimeUnit.SECONDS);
                verify(repository).saveAll(List.of());
            } finally {
                release.countDown();
            }
        }
    }
}
