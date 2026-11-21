package com.kalibra.api.progress.domain.model.valueobjects;

import java.util.UUID;

public record SubtopicApprovalLine(
        UUID subtopicId,
        String subtopicName,
        double approvalRate
) {
}
