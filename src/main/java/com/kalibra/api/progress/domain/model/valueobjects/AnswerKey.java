package com.kalibra.api.progress.domain.model.valueobjects;

public record AnswerKey(
        String statement,
        String correctOptionKey,
        String explanation,
        SubtopicId subtopicId
) {

    public boolean isCorrect(String selectedOptionKey) {
        return selectedOptionKey != null
                && correctOptionKey.equalsIgnoreCase(selectedOptionKey.trim());
    }
}
