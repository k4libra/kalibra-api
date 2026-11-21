package com.kalibra.api.progress.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

public record SubtopicGapResource(
        UUID subtopicId,
        String subtopicName,
        @Schema(description = "Average mastery of the students with data, as a percentage 0..100")
        double groupMastery,
        int lowCount,
        int mediumCount,
        int highCount,
        int noDataCount,
        @Schema(description = "1 is the subtopic to reinforce first")
        int reinforcementPriority
) {
}
