package com.kalibra.api.enrollment.interfaces.rest.resources;

import java.time.Instant;
import java.util.UUID;

public record PendingInvitationResource(
        UUID id,
        String courseName,
        String teacherEmail,
        Instant sentAt,
        UUID courseId,
        String teacherName,
        String status,
        Instant expiresAt
) {
}