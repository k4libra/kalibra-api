package com.kalibra.api.enrollment.domain.model.events;

import java.util.UUID;

public record InvitationSent(
        UUID invitationId,
        UUID courseId,
        UUID studentId
) {}