package com.kalibra.api.progress.interfaces.rest.resources;

import java.util.UUID;

public record RequestPracticeExerciseResource(
        UUID courseId,
        UUID subtopicId
) {
}