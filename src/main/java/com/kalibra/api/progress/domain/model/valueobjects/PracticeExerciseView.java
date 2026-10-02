package com.kalibra.api.progress.domain.model.valueobjects;

import java.util.List;
import java.util.UUID;

public record PracticeExerciseView(
        UUID exerciseId,
        UUID subtopicId,
        String statement,
        List<String> options
) {
}