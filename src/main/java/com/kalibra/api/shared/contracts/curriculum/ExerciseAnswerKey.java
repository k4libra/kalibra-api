package com.kalibra.api.shared.contracts.curriculum;

import java.util.List;
import java.util.UUID;

public record ExerciseAnswerKey(UUID exerciseId, UUID subtopicId, String statement, String correctOptionKey, String explanation,
        List<String> options, List<ExplanationStep> explanationSteps, String keyIdea) {
    public ExerciseAnswerKey(UUID exerciseId, UUID subtopicId, String statement, String correctOptionKey, String explanation) {
        this(exerciseId, subtopicId, statement, correctOptionKey, explanation, List.of(), List.of(), null);
    }
}
