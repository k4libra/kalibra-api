package com.kalibra.api.progress.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

public record CriticalSubtopicResource(
        UUID courseId,
        String courseName,
        UUID subtopicId,
        String subtopicName,
        @Schema(description = "Group mastery as a percentage 0..100") double groupMastery,
        int lowCount,
        int mediumCount,
        int highCount,
        int noDataCount
) {
}
