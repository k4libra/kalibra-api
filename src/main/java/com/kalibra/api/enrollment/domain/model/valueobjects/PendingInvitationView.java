package com.kalibra.api.enrollment.domain.model.valueobjects;

import java.time.Instant;
import java.util.UUID;

public record PendingInvitationView(
        UUID invitationId,
        String courseName,
        String teacherEmail,
        Instant sentAt,
        UUID courseId,
        String teacherName,
        String status,
        Instant expiresAt
) {
    public PendingInvitationView(UUID invitationId, String courseName, String teacherEmail, Instant sentAt, Instant expiresAt) {
        this(invitationId, courseName, teacherEmail, sentAt, null, null, "PENDING", expiresAt);
    }
}