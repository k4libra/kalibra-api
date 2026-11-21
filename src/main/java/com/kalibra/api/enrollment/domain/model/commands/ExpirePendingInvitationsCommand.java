package com.kalibra.api.enrollment.domain.model.commands;

import java.time.Instant;

public record ExpirePendingInvitationsCommand(
        Instant now
) {
}