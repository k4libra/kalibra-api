package com.kalibra.api.progress.interfaces.rest.resources;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RequestPracticeExerciseResource(
        @NotNull UUID courseId,
        @NotNull UUID subtopicId
) {
}
