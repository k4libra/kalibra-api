package com.kalibra.api.enrollment.domain.model.commands;

import com.kalibra.api.enrollment.domain.model.valueobjects.InvitationId;

public record AcceptInvitationCommand(
        String holderId,
        InvitationId invitationId
) {
}