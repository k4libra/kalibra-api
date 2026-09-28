package com.kalibra.api.progress.application.internal.eventhandlers;

import com.kalibra.api.progress.domain.model.commands.UpdateSubtopicMasteryCommand;
import com.kalibra.api.progress.domain.model.events.AnswerRecorded;
import com.kalibra.api.progress.domain.model.valueobjects.*;
import com.kalibra.api.progress.domain.services.SubtopicMasteryCommandService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class AnswerRecordedEventHandler {

    private final SubtopicMasteryCommandService subtopicMasteryCommandService;

    public AnswerRecordedEventHandler(
            SubtopicMasteryCommandService subtopicMasteryCommandService) {
        this.subtopicMasteryCommandService = subtopicMasteryCommandService;
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

        subtopicMasteryCommandService.handle(command);
    }
}