package com.kalibra.api.progress.domain.model.valueobjects;

public record InstitutionalIndicatorTotals(
        int courses,
        int teachers,
        int enrolledStudents,
        int activeStudents,
        int totalSolved,
        double groupAccuracy,
        int groupDeltaPoints,
        double verificationApprovalRate
) {
}
