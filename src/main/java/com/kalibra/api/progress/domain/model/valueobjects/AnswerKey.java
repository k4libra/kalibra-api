package com.kalibra.api.progress.domain.model.valueobjects;

import com.kalibra.api.shared.contracts.curriculum.ExplanationStep;

import java.util.List;

public record AnswerKey(
        String statement,
        String correctOptionKey,
        String explanation,
        SubtopicId subtopicId,
        List<String> options,
        List<ExplanationStep> explanationSteps,
        String keyIdea
) {

    public AnswerKey(String statement, String correctOptionKey, String explanation, SubtopicId subtopicId) {
        this(statement, correctOptionKey, explanation, subtopicId, List.of(), List.of(), null);
    }

    public boolean isCorrect(String selectedOptionKey) {
        return selectedOptionKey != null
                && correctOptionKey.equalsIgnoreCase(selectedOptionKey.trim());
    }
}
