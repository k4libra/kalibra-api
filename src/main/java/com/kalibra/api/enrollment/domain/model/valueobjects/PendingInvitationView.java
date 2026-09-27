package com.kalibra.api.enrollment.domain.model.valueobjects;

import java.time.Instant;
import java.util.UUID;

public record PendingInvitationView(
        UUID invitationId,
        String courseName,
        String teacherEmail,
        Instant sentAt,
        Instant expiresAt
) {
}