package com.kalibra.api.progress.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

public record StudentProgressResource(
        UUID studentId,
        UUID courseId,
        @Schema(description = "false while the student has not answered any exercise of the course")
        boolean hasActivity,
        List<SubtopicProgressResource> subtopics,
        @Schema(description = "Explanations of the latest answers, newest first")
        List<String> recentFeedback
) {
}
