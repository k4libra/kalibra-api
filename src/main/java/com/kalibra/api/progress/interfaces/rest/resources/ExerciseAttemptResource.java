package com.kalibra.api.progress.interfaces.rest.resources;

import com.kalibra.api.shared.contracts.curriculum.ExplanationStep;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
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
        Integer timeSpentSeconds, String selectedOptionKey, String correctOptionKey,
        List<String> options, List<ExplanationStep> explanationSteps, String keyIdea,
        Instant answeredAt
) {
}
