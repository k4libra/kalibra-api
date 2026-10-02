package com.kalibra.api.progress.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

public record SubtopicEvolutionResource(
        UUID subtopicId,
        String subtopicName,
        @Schema(description = "null for a subtopic without practice")
        Double initialAverage,
        @Schema(description = "null for a subtopic without practice")
        Double currentAverage,
        @Schema(description = "Change in percentage points; null for a subtopic without practice")
        Integer deltaPoints,
        @Schema(allowableValues = {"LOW", "MEDIUM", "HIGH", "NO_DATA"})
        String level,
        @Schema(description = "false means nobody has solved an exercise of the subtopic yet")
        boolean practiced
) {
}
