package com.kalibra.api.enrollment.domain.services;

import com.kalibra.api.enrollment.domain.model.aggregates.Invitation;
import com.kalibra.api.enrollment.domain.model.commands.*;

public interface InvitationCommandService {

    Invitation handle(SendInvitationCommand command);

    Invitation handle(CancelInvitationCommand command);

    Invitation handle(ResendInvitationCommand command);

    Invitation handle(AcceptInvitationCommand command);

    Invitation handle(RejectInvitationCommand command);

    int handle(ExpirePendingInvitationsCommand command);
}