package com.kalibra.api.progress.application.internal.eventhandlers;

import com.kalibra.api.progress.application.internal.outboundservices.cache.GapMapCacheService;
import com.kalibra.api.progress.domain.model.commands.UpdateSubtopicMasteryCommand;
import com.kalibra.api.progress.domain.model.events.AnswerRecorded;
import com.kalibra.api.progress.domain.model.valueobjects.CourseId;
import com.kalibra.api.progress.domain.model.valueobjects.MasteryEstimate;
import com.kalibra.api.progress.domain.model.valueobjects.MasteryLevel;
import com.kalibra.api.progress.domain.model.valueobjects.MasteryProbability;
import com.kalibra.api.progress.domain.model.valueobjects.SubtopicId;
import com.kalibra.api.progress.domain.services.SubtopicMasteryCommandService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class AnswerRecordedEventHandler {

    private final SubtopicMasteryCommandService subtopicMasteryCommandService;
    private final GapMapCacheService gapMapCacheService;

    public AnswerRecordedEventHandler(
            SubtopicMasteryCommandService subtopicMasteryCommandService,
            GapMapCacheService gapMapCacheService) {
        this.subtopicMasteryCommandService = subtopicMasteryCommandService;
        this.gapMapCacheService = gapMapCacheService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(AnswerRecorded event) {
        var estimate = new MasteryEstimate(
                new MasteryProbability(event.probability()),
                MasteryLevel.valueOf(event.level())
        );
        var command = new UpdateSubtopicMasteryCommand(
                event.holderId(),
                new CourseId(event.courseId()),
                new SubtopicId(event.subtopicId()),
                estimate
        );
        var mastery = subtopicMasteryCommandService.handle(command);
        // MasteryUpdated carries no course, so the cached gap map is dropped here, once the
        // mastery update has committed.
        gapMapCacheService.evict(mastery.getCourseId());
    }
}
