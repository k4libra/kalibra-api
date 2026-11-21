package com.kalibra.api.progress.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record VerificationIndicatorResource(
        @Schema(description = "Approved exercises / generated exercises x 100")
        double approvalRate,
        int approved,
        int discarded,
        @Schema(description = "Only the subtopics with generated exercises")
        List<SubtopicApprovalResource> perSubtopic
) {
}
