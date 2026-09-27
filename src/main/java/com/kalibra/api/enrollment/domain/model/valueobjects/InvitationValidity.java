package com.kalibra.api.enrollment.domain.model.valueobjects;

import java.time.Instant;

public record InvitationValidity(
        Instant sentAt,
        Instant expiresAt
) {

    public static InvitationValidity threeDaysFrom(Instant sentAt) {
        return new InvitationValidity(
                sentAt,
                sentAt.plusSeconds(3 * 24 * 60 * 60)
        );
    }

    public boolean hasExpired(Instant now) {
        return !now.isBefore(expiresAt);
    }
}