package com.kalibra.api.progress.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

public record MasteryGapMapResource(
        UUID courseId,
        @Schema(description = "false while no enrolled student has a mastery estimate")
        boolean hasSufficientData,
        @Schema(description = "Subtopics ordered by reinforcement priority")
        List<SubtopicGapResource> subtopics,
        @Schema(description = "One cell per enrolled student and subtopic")
        List<StudentMasteryResource> students
) {
}
