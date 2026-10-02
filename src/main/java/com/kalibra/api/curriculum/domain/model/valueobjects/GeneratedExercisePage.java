package com.kalibra.api.curriculum.domain.model.valueobjects;

import com.kalibra.api.curriculum.domain.model.aggregates.GeneratedExercise;

import java.util.List;

public record GeneratedExercisePage(List<GeneratedExercise> items, int page, int size, long totalElements, int totalPages) {

    public GeneratedExercisePage {
        items = List.copyOf(items);
    }
}
