package com.kalibra.api.progress.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

public record StudentMasteryResource(
        UUID studentId,
        String email,
        UUID subtopicId,
        @Schema(description = "Mastery as a percentage 0..100; null without data")
        Double mastery,
        @Schema(allowableValues = {"LOW", "MEDIUM", "HIGH", "NO_DATA"})
        String level
) {
}
