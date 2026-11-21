package com.kalibra.api.progress.application.internal.eventhandlers;

import com.kalibra.api.progress.ProgressFixtures;
import com.kalibra.api.progress.application.internal.outboundservices.cache.GapMapCacheService;
import com.kalibra.api.progress.domain.model.commands.UpdateSubtopicMasteryCommand;
import com.kalibra.api.progress.domain.model.events.AnswerRecorded;
import com.kalibra.api.progress.domain.model.valueobjects.CourseId;
import com.kalibra.api.progress.domain.model.valueobjects.MasteryEstimate;
import com.kalibra.api.progress.domain.model.valueobjects.MasteryLevel;
import com.kalibra.api.progress.domain.model.valueobjects.MasteryProbability;
import com.kalibra.api.progress.domain.model.valueobjects.SubtopicId;
import com.kalibra.api.progress.domain.services.SubtopicMasteryCommandService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnswerRecordedEventHandlerTest {

    @Mock
    SubtopicMasteryCommandService subtopicMasteryCommandService;

    @Mock
    GapMapCacheService gapMapCacheService;

    @InjectMocks
    AnswerRecordedEventHandler handler;

    private final String holderId = UUID.randomUUID().toString();
    private final CourseId courseId = new CourseId(UUID.randomUUID());
    private final SubtopicId subtopicId = new SubtopicId(UUID.randomUUID());
    private final AnswerRecorded event =
            new AnswerRecorded(UUID.randomUUID(), holderId, courseId.value(), subtopicId.value(), 0.63, "MEDIUM");
    private final UpdateSubtopicMasteryCommand command = new UpdateSubtopicMasteryCommand(holderId, courseId, subtopicId,
            new MasteryEstimate(new MasteryProbability(0.63), MasteryLevel.MEDIUM));

    @Test
    void shouldUpdateTheMasteryAndThenDropTheCachedGapMapOfTheCourse() {
        when(subtopicMasteryCommandService.handle(command))
                .thenReturn(ProgressFixtures.mastery(holderId, courseId, subtopicId, 0.30, 0.63, MasteryLevel.MEDIUM));

        handler.on(event);

        InOrder order = inOrder(subtopicMasteryCommandService, gapMapCacheService);
        order.verify(subtopicMasteryCommandService).handle(command);
        order.verify(gapMapCacheService).evict(courseId);
    }

    @Test
    void shouldKeepTheCacheWhenTheMasteryUpdateFails() {
        when(subtopicMasteryCommandService.handle(command)).thenThrow(new IllegalStateException("database down"));

        assertThatThrownBy(() -> handler.on(event)).isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(gapMapCacheService);
    }

    @Test
    void shouldListenOnlyAfterTheAnswerHasCommitted() throws NoSuchMethodException {
        var listener = AnswerRecordedEventHandler.class.getMethod("on", AnswerRecorded.class)
                .getAnnotation(TransactionalEventListener.class);

        assertThat(listener.phase()).isEqualTo(TransactionPhase.AFTER_COMMIT);
    }
}
