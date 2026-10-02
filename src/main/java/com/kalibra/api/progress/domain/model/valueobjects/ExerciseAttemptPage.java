package com.kalibra.api.progress.domain.model.valueobjects;

import com.kalibra.api.progress.domain.model.aggregates.ExerciseAttempt;

import java.util.List;

public record ExerciseAttemptPage(
        List<ExerciseAttempt> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public ExerciseAttemptPage {
        items = List.copyOf(items);
    }
}
