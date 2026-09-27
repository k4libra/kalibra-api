package com.kalibra.api.enrollment.domain.model.valueobjects;

import java.util.List;
import java.util.UUID;

public record CourseRosterGroup(
        UUID courseId,
        String courseName,
        String courseCode,
        int enrolledCount,
        List<RosterLine> students
) {
}