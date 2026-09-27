package com.kalibra.api.enrollment.domain.model.valueobjects;

import java.time.Instant;
import java.util.UUID;

public record RosterLine(
        UUID studentId,
        String email,
        Instant enrolledAt
) {
}