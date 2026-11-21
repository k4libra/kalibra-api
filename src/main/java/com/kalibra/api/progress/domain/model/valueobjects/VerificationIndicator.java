package com.kalibra.api.progress.domain.model.valueobjects;

import java.util.List;

public record VerificationIndicator(
        double approvalRate,
        int approved,
        int discarded,
        List<SubtopicApprovalLine> perSubtopic
) {

    public VerificationIndicator {
        perSubtopic = List.copyOf(perSubtopic);
    }
}
