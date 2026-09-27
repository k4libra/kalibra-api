package com.kalibra.api.enrollment.application.internal.outboundservices.notifications;

import com.kalibra.api.enrollment.domain.model.aggregates.Invitation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PushInvitationNotificationService implements InvitationNotificationService {

    private static final Logger log = LoggerFactory.getLogger(PushInvitationNotificationService.class);

    @Override
    public void notifyInvitation(Invitation invitation) {
        log.info("Course invitation {} due for student {}",
                invitation.getId().value(), invitation.getStudentId().value());
    }
}
