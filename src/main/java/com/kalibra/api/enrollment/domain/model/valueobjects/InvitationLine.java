package com.kalibra.api.enrollment.domain.model.valueobjects;

import java.time.Instant;
import java.util.UUID;

public record InvitationLine(
        UUID invitationId,
        String invitedEmail,
        InvitationStatus status,
        Instant sentAt,
        Instant expiresAt
) {
}
