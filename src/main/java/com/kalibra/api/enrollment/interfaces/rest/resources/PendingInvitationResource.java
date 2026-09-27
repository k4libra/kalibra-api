package com.kalibra.api.enrollment.interfaces.rest.resources;

import java.time.Instant;
import java.util.UUID;

public record PendingInvitationResource(
        UUID id,
        String courseName,
        String teacherEmail,
        Instant sentAt,
        Instant expiresAt
) {
}