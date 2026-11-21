package com.kalibra.api.progress.interfaces.rest.resources;

import java.util.UUID;

public record SubtopicApprovalResource(
        UUID subtopicId,
        String subtopicName,
        double approvalRate
) {
}
