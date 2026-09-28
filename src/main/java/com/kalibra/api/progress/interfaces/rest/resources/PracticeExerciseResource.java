package com.kalibra.api.progress.interfaces.rest.resources;

import java.util.List;
import java.util.UUID;

public record PracticeExerciseResource(
        UUID exerciseId,
        UUID subtopicId,
        String statement,
        List<String> options
) {
}