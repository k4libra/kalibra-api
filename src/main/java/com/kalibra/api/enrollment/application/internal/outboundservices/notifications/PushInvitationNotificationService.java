package com.kalibra.api.enrollment.application.internal.outboundservices.notifications;

import com.kalibra.api.enrollment.domain.model.aggregates.Invitation;
import org.springframework.stereotype.Service;

@Service
public class PushInvitationNotificationService
        implements InvitationNotificationService {

    @Override
    public void notifyInvitation(Invitation invitation) {
        // Implementación de push móvil pendiente.
    }
}