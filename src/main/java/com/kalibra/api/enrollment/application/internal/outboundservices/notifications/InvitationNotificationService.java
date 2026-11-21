package com.kalibra.api.enrollment.application.internal.outboundservices.notifications;

import com.kalibra.api.enrollment.domain.model.aggregates.Invitation;

public interface InvitationNotificationService {

    void notifyInvitation(Invitation invitation);
}