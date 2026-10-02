package com.kalibra.api.progress.domain.model.valueobjects;

import java.time.Instant;
import java.util.UUID;

public record AttemptHistoryLine(
        UUID attemptId,
        UUID subtopicId,
        String statement,
        AnswerResult result,
        Feedback feedback,
        MasteryChange masteryChange,
        Instant answeredAt
) {
}
