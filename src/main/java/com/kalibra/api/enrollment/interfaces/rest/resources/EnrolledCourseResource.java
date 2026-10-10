package com.kalibra.api.enrollment.interfaces.rest.resources;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record EnrolledCourseResource(
        UUID courseId,
        String name,
        String code,
        String teacherName,
        List<EnrolledSubtopicResource> subtopics,
        Instant enrolledAt
) {
}
