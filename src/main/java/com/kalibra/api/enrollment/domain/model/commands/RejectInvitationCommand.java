package com.kalibra.api.enrollment.domain.model.commands;

import com.kalibra.api.enrollment.domain.model.valueobjects.InvitationId;

public record RejectInvitationCommand(
        String holderId,
        InvitationId invitationId
) {
}