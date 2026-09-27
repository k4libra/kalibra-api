package com.kalibra.api.enrollment.domain.model.aggregates;

import com.kalibra.api.enrollment.domain.model.commands.*;
import com.kalibra.api.enrollment.domain.exceptions.InvitationNotPendingException;
import com.kalibra.api.enrollment.domain.exceptions.InvitationNotResendableException;
import com.kalibra.api.enrollment.domain.model.valueobjects.*;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

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
        Invitation invitation = new Invitation();

        invitation.id = new InvitationId(UUID.randomUUID());
        invitation.courseId = command.courseId();
        invitation.holderId = command.holderId();
        invitation.studentId = studentId;
        invitation.invitedEmail = command.studentEmail();
        invitation.status = InvitationStatus.PENDING;
        invitation.validity = InvitationValidity.threeDaysFrom(Instant.now());
        invitation.respondedAt = Optional.empty();

        return invitation;
    }

    public void cancel(CancelInvitationCommand command) {
        if (status != InvitationStatus.PENDING) {
            throw new InvitationNotPendingException();
        }

        status = InvitationStatus.CANCELED;
        respondedAt = Optional.of(Instant.now());
    }

    public void resend(ResendInvitationCommand command) {
        if (status != InvitationStatus.PENDING) {
            throw new InvitationNotResendableException();
        }

        validity = InvitationValidity.threeDaysFrom(Instant.now());
    }

    public void accept(AcceptInvitationCommand command) {
        if (status != InvitationStatus.PENDING) {
            throw new InvitationNotPendingException();
        }

        status = InvitationStatus.ACCEPTED;
        respondedAt = Optional.of(Instant.now());
    }

    public void reject(RejectInvitationCommand command) {
        if (status != InvitationStatus.PENDING) {
            throw new InvitationNotPendingException();
        }

        status = InvitationStatus.REJECTED;
        respondedAt = Optional.of(Instant.now());
    }

    public void expire(Instant now) {
        if (status == InvitationStatus.PENDING && validity.hasExpired(now)) {
            status = InvitationStatus.EXPIRED;
        }
    }

    public InvitationId getId() {
        return id;
    }

    public CourseId getCourseId() {
        return courseId;
    }

    public String getHolderId() {
        return holderId;
    }

    public StudentId getStudentId() {
        return studentId;
    }

    public Email getInvitedEmail() {
        return invitedEmail;
    }

    public InvitationStatus getStatus() {
        return status;
    }

    public InvitationValidity getValidity() {
        return validity;
    }

    public Optional<Instant> getRespondedAt() {
        return respondedAt;
    }
}