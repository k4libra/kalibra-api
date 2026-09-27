package com.kalibra.api.enrollment.domain.model.events;

import java.util.UUID;

public record StudentEnrolled(
        UUID enrollmentId,
        UUID courseId,
        UUID studentId
) {
}