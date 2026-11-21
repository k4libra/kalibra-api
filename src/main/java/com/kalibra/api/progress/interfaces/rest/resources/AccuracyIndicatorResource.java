package com.kalibra.api.progress.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record AccuracyIndicatorResource(
        @Schema(description = "Correct answers / submitted answers x 100 over the students with activity")
        double groupAccuracy,
        List<StudentAccuracyResource> perStudent
) {
}
