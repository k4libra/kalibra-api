package com.kalibra.api.shared.contracts.enrollment;

import java.time.Instant;
import java.util.UUID;

public record RosterEntry(
        UUID studentId,
        String email,
        Instant enrolledAt
) {
}