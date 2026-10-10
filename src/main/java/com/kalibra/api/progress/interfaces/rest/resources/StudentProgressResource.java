package com.kalibra.api.progress.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record StudentProgressResource(
        UUID studentId,
        UUID courseId,
        @Schema(description = "false while the student has not answered any exercise of the course")
        boolean hasActivity,
        @Schema(description = "Latest answer timestamp in this course; null when no exercise has been answered")
        Instant lastActivityAt,
        List<SubtopicProgressResource> subtopics,
        @Schema(description = "Explanations of the latest answers, newest first")
        List<String> recentFeedback
) {
}
