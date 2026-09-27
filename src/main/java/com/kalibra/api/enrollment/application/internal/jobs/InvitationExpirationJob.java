package com.kalibra.api.enrollment.application.internal.jobs;

import com.kalibra.api.enrollment.domain.model.commands.ExpirePendingInvitationsCommand;
import com.kalibra.api.enrollment.domain.services.InvitationCommandService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class InvitationExpirationJob {

    private final InvitationCommandService invitationCommandService;

    public InvitationExpirationJob(
            InvitationCommandService invitationCommandService
    ) {
        this.invitationCommandService = invitationCommandService;
    }

    @Scheduled(fixedDelay = 60000)
    public void expirePendingInvitations() {
        invitationCommandService.handle(
                new ExpirePendingInvitationsCommand(Instant.now())
        );
    }
}