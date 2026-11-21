package com.kalibra.api.progress.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

public record CourseIndicatorReportResource(
        UUID courseId,
        @Schema(description = "false while no exercise has been solved in the course: show an empty state and do not offer the export")
        boolean hasActivity,
        AccuracyIndicatorResource accuracy,
        PracticeIndicatorResource practice,
        MasteryEvolutionResource evolution,
        VerificationIndicatorResource verification
) {
}
