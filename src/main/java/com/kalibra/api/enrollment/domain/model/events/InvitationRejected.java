package com.kalibra.api.enrollment.domain.model.events;

import java.util.UUID;

public record InvitationRejected(
        UUID invitationId
) {}