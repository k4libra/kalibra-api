package com.kalibra.api.progress.domain.model.valueobjects;

import java.util.UUID;

public record CourseIndicatorSummary(
        UUID courseId,
        String name,
        String code,
        String teacherName,
        boolean hasActivity,
        int enrolledStudents,
        int activeStudents,
        int totalSolved,
        double groupAccuracy,
        int groupDeltaPoints,
        double verificationApprovalRate
) {
}
