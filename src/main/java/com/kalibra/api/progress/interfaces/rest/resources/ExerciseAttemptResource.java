package com.kalibra.api.progress.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

public record ExerciseAttemptResource(
        UUID id,
        UUID subtopicId,
        String statement,
        @Schema(allowableValues = {"CORRECT", "INCORRECT"})
        String result,
        String explanation,
        @Schema(description = "Mastery of the subtopic before this answer, as a percentage 0..100; null when unknown")
        Double previousMastery,
        @Schema(description = "Mastery of the subtopic after this answer, as a percentage 0..100")
        double currentMastery,
        @Schema(description = "Percentage points the mastery went up (positive) or down (negative) with this answer")
        int masteryDeltaPoints,
        @Schema(description = "false when the mastery stayed the same")
        boolean masteryChanged,
        Instant answeredAt
) {
}
