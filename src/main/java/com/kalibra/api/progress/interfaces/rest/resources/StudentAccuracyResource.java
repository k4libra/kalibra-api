package com.kalibra.api.progress.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

public record StudentAccuracyResource(
        UUID studentId,
        String email,
        int correct,
        int submitted,
        @Schema(description = "Percentage 0..100; null for a student without activity")
        Double accuracy,
        boolean hasActivity
) {
}
