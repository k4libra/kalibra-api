package com.kalibra.api.progress.application.internal.commandservices;

import com.kalibra.api.progress.ProgressFixtures;
import com.kalibra.api.progress.domain.model.aggregates.SubtopicMastery;
import com.kalibra.api.progress.domain.model.commands.UpdateSubtopicMasteryCommand;
import com.kalibra.api.progress.domain.model.events.MasteryUpdated;
import com.kalibra.api.progress.domain.model.valueobjects.CourseId;
import com.kalibra.api.progress.domain.model.valueobjects.MasteryEstimate;
import com.kalibra.api.progress.domain.model.valueobjects.MasteryLevel;
import com.kalibra.api.progress.domain.model.valueobjects.MasteryProbability;
import com.kalibra.api.progress.domain.model.valueobjects.SubtopicId;
import com.kalibra.api.progress.domain.repositories.SubtopicMasteryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubtopicMasteryCommandServiceImplTest {

    @Mock
    SubtopicMasteryRepository subtopicMasteryRepository;

    @Mock
    ApplicationEventPublisher eventPublisher;

    @Mock
    PlatformTransactionManager transactionManager;

    SubtopicMasteryCommandServiceImpl service;

    private final String holderId = UUID.randomUUID().toString();
    private final CourseId courseId = new CourseId(UUID.randomUUID());
    private final SubtopicId subtopicId = new SubtopicId(UUID.randomUUID());
    private final UpdateSubtopicMasteryCommand command = new UpdateSubtopicMasteryCommand(holderId, courseId, subtopicId,
            new MasteryEstimate(new MasteryProbability(0.63), MasteryLevel.MEDIUM));

    @BeforeEach
    void setUp() {
        service = new SubtopicMasteryCommandServiceImpl(subtopicMasteryRepository, eventPublisher, transactionManager);
    }

    @Test
    void shouldInitializeTheMasteryOnTheFirstEstimateAndPublishIt() {
        // Arrange
        var event = ArgumentCaptor.forClass(MasteryUpdated.class);
        when(subtopicMasteryRepository.findByHolderIdAndSubtopicId(holderId, subtopicId)).thenReturn(Optional.empty());
        when(subtopicMasteryRepository.save(any(SubtopicMastery.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        var mastery = service.handle(command);

        // Assert
        assertThat(mastery.getInitialEstimate().value()).isEqualTo(0.63);
        assertThat(mastery.getEstimatesCount()).isEqualTo(1);
        assertThat(mastery.getCourseId()).isEqualTo(courseId);
        verify(eventPublisher).publishEvent(event.capture());
        assertThat(event.getValue()).isEqualTo(new MasteryUpdated(holderId, subtopicId.value(), 0.63));
    }

    @Test
    void shouldApplyTheEstimateOverTheExistingMasteryKeepingItsBaseline() {
        var existing = ProgressFixtures.mastery(holderId, courseId, subtopicId, 0.30, 0.41, MasteryLevel.MEDIUM);
        when(subtopicMasteryRepository.findByHolderIdAndSubtopicId(holderId, subtopicId)).thenReturn(Optional.of(existing));
        when(subtopicMasteryRepository.save(existing)).thenReturn(existing);

        var mastery = service.handle(command);

        assertThat(mastery.getInitialEstimate().value()).isEqualTo(0.30);
        assertThat(mastery.getCurrentEstimate().value()).isEqualTo(0.63);
        assertThat(mastery.getEstimatesCount()).isEqualTo(3);
        verify(eventPublisher).publishEvent(any(MasteryUpdated.class));
    }

    @Test
    void shouldRunInItsOwnTransactionBecauseItIsCalledAfterACommit() {
        var definition = ArgumentCaptor.forClass(TransactionDefinition.class);
        when(subtopicMasteryRepository.findByHolderIdAndSubtopicId(holderId, subtopicId)).thenReturn(Optional.empty());
        when(subtopicMasteryRepository.save(any(SubtopicMastery.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.handle(command);

        verify(transactionManager, atLeastOnce()).getTransaction(definition.capture());
        assertThat(definition.getValue().getPropagationBehavior()).isEqualTo(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Test
    void shouldApplyOverTheMasteryCreatedByAConcurrentFirstAnswer() {
        // Arrange
        var concurrent = ProgressFixtures.mastery(holderId, courseId, subtopicId, 0.30, 0.41, MasteryLevel.MEDIUM);
        when(subtopicMasteryRepository.findByHolderIdAndSubtopicId(holderId, subtopicId))
                .thenReturn(Optional.empty(), Optional.of(concurrent));
        when(subtopicMasteryRepository.save(any(SubtopicMastery.class)))
                .thenThrow(new DataIntegrityViolationException("uq_subtopic_masteries_holder_subtopic"))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        var mastery = service.handle(command);

        // Assert
        assertThat(mastery).isSameAs(concurrent);
        assertThat(mastery.getCurrentEstimate().value()).isEqualTo(0.63);
        assertThat(mastery.getInitialEstimate().value()).isEqualTo(0.30);
    }
}
