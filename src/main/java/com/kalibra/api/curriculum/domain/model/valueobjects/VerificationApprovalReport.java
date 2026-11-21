package com.kalibra.api.curriculum.domain.model.valueobjects;

import java.util.List;

public record VerificationApprovalReport(
        int generated,
        int approved,
        int discarded,
        double approvalRate,
        List<SubtopicExerciseCount> bySubtopic
) {

    public VerificationApprovalReport {
        bySubtopic = List.copyOf(bySubtopic);
    }
}
