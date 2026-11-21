package com.kalibra.api.progress.domain.model.events;

import java.util.UUID;

public record AnswerRecorded(
        UUID attemptId,
        String holderId,
        UUID courseId,
        UUID subtopicId,
        double probability,
        String level
) {
}