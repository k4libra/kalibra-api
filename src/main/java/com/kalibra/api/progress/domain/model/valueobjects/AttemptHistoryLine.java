package com.kalibra.api.progress.domain.model.valueobjects;

import com.kalibra.api.shared.contracts.curriculum.ExplanationStep;
import com.kalibra.api.shared.exercises.ExplanationSteps;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AttemptHistoryLine(
        UUID attemptId,
        UUID subtopicId,
        String statement,
        AnswerResult result,
        Feedback feedback,
        MasteryChange masteryChange,
        Instant answeredAt,
        Integer timeSpentSeconds, String selectedOptionKey, String correctOptionKey,
        List<String> options, List<ExplanationStep> explanationSteps, String keyIdea
) {
    public AttemptHistoryLine(UUID attemptId, UUID subtopicId, String statement, AnswerResult result,
            Feedback feedback, MasteryChange masteryChange, Instant answeredAt) {
        this(attemptId, subtopicId, statement, result, feedback, masteryChange, answeredAt,
                null, null, null, List.of(),
                ExplanationSteps.resolve(null, feedback == null ? null : feedback.explanation()), null);
    }
}
