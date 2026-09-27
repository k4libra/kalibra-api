package com.kalibra.api.enrollment.interfaces.rest.resources;

import java.time.Instant;
import java.util.UUID;

public record RosterStudentResource(
        UUID studentId,
        String email,
        Instant enrolledAt
) {
}