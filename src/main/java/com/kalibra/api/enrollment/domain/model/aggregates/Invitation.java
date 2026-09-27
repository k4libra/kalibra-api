package com.kalibra.api.enrollment.domain.model.aggregates;

import com.kalibra.api.enrollment.domain.model.commands.AcceptInvitationCommand;
import com.kalibra.api.enrollment.domain.model.commands.CancelInvitationCommand;
import com.kalibra.api.enrollment.domain.model.commands.RejectInvitationCommand;
import com.kalibra.api.enrollment.domain.model.commands.ResendInvitationCommand;
import com.kalibra.api.enrollment.domain.model.commands.SendInvitationCommand;
import com.kalibra.api.enrollment.domain.model.valueobjects.*;

import java.time.Instant;
import java.util.Optional;

public class Invitation {

    private InvitationId id;
    private CourseId courseId;
    private String holderId;
    private StudentId studentId;
    private Email invitedEmail;
    private InvitationStatus status;
    private InvitationValidity validity;
    private Optional<Instant> respondedAt;

    public static Invitation send(
            SendInvitationCommand command,
            StudentId studentId
    ) {
        return null;
    }

    public void cancel(CancelInvitationCommand command) {
    }

    public void resend(ResendInvitationCommand command) {
    }

    public void accept(AcceptInvitationCommand command) {
    }

    public void reject(RejectInvitationCommand command) {
    }

    public void expire(Instant now) {
    }
}