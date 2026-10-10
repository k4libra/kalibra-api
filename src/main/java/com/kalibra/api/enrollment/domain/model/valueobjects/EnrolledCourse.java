package com.kalibra.api.enrollment.domain.model.valueobjects;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record EnrolledCourse(
        UUID courseId,
        String name,
        String code,
        String teacherName,
        List<EnrolledSubtopic> subtopics,
        Instant enrolledAt
) {

    public record EnrolledSubtopic(UUID id, String name, int displayOrder) {
    }
}
