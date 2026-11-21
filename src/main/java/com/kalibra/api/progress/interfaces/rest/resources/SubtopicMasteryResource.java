package com.kalibra.api.progress.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

public record SubtopicMasteryResource(
        UUID subtopicId,
        String name,
        @Schema(allowableValues = {"LOW", "MEDIUM", "HIGH", "NO_DATA"},
                description = "NO_DATA until the student answers an exercise of the subtopic")
        String level
) {
}
