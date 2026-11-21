package com.kalibra.api.enrollment.domain.model.queries;

public record GetPendingInvitationsByHolderIdQuery(
        String holderId,
        com.kalibra.api.enrollment.domain.model.valueobjects.InvitationStatus status
) {
    public GetPendingInvitationsByHolderIdQuery(String holderId) {
        this(holderId, com.kalibra.api.enrollment.domain.model.valueobjects.InvitationStatus.PENDING);
    }
}