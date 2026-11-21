package com.kalibra.api.enrollment.application.internal.jobs;

import com.kalibra.api.enrollment.domain.model.commands.ExpirePendingInvitationsCommand;
import com.kalibra.api.enrollment.domain.services.InvitationCommandService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class InvitationExpirationJobTest {

    @Mock
    InvitationCommandService invitationCommandService;

    @InjectMocks
    InvitationExpirationJob job;

    @Test
    void shouldExpireThePendingInvitationsPastTheirValidity() {
        job.expirePendingInvitations();

        verify(invitationCommandService).handle(any(ExpirePendingInvitationsCommand.class));
    }
}
