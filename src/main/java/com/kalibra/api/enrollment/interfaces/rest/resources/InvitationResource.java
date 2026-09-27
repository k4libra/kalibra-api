package com.kalibra.api.enrollment.interfaces.rest.resources;

import java.time.Instant;
import java.util.UUID;

public record InvitationResource(
        UUID id,
        UUID courseId,
        String invitedEmail,
        String status,
        Instant sentAt,
        Instant expiresAt
) {
}