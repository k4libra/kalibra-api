package com.kalibra.api.enrollment.interfaces.rest.resources;

import java.util.List;
import java.util.UUID;

public record CourseRosterResource(
        UUID courseId,
        String courseName,
        String courseCode,
        int enrolledCount,
        List<RosterStudentResource> students
) {
}