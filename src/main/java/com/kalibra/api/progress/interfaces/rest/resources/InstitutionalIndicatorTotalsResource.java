package com.kalibra.api.progress.interfaces.rest.resources;

public record InstitutionalIndicatorTotalsResource(
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
