package com.kalibra.api.progress.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

public record SubtopicProgressResource(
        UUID subtopicId,
        String subtopicName,
        @Schema(description = "Mastery as a percentage 0..100; null without answers in the subtopic")
        Double mastery,
        @Schema(allowableValues = {"LOW", "MEDIUM", "HIGH", "NO_DATA"})
        String level,
        int solvedCount
) {
}
