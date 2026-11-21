package com.kalibra.api.progress.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

public record CourseIndicatorSummaryResource(
        UUID courseId,
        String name,
        String code,
        String teacherName,
        boolean hasActivity,
        int enrolledStudents,
        int activeStudents,
        int totalSolved,
        @Schema(description = "Percentage 0..100") double groupAccuracy,
        @Schema(description = "Percentage points between initial and current group mastery") int groupDeltaPoints,
        @Schema(description = "Percentage 0..100 of generated exercises approved by verification") double verificationApprovalRate
) {
}
