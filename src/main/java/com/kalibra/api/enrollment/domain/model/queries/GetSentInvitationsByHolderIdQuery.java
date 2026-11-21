package com.kalibra.api.enrollment.domain.model.queries;

import com.kalibra.api.enrollment.domain.model.valueobjects.InvitationStatus;

import java.util.Optional;

public record GetSentInvitationsByHolderIdQuery(
        String holderId,
        Optional<InvitationStatus> status
) {
}
