package com.kalibra.api.enrollment.domain.model.aggregates;

import com.kalibra.api.enrollment.domain.exceptions.InvitationNotPendingException;
import com.kalibra.api.enrollment.domain.exceptions.InvitationNotResendableException;
import com.kalibra.api.enrollment.domain.model.commands.AcceptInvitationCommand;
import com.kalibra.api.enrollment.domain.model.commands.CancelInvitationCommand;
import com.kalibra.api.enrollment.domain.model.commands.RejectInvitationCommand;
import com.kalibra.api.enrollment.domain.model.commands.ResendInvitationCommand;
import com.kalibra.api.enrollment.domain.model.commands.SendInvitationCommand;
import com.kalibra.api.enrollment.domain.model.valueobjects.CourseId;
import com.kalibra.api.enrollment.domain.model.valueobjects.Email;
import com.kalibra.api.enrollment.domain.model.valueobjects.InvitationId;
import com.kalibra.api.enrollment.domain.model.valueobjects.InvitationStatus;
import com.kalibra.api.enrollment.domain.model.valueobjects.InvitationValidity;
import com.kalibra.api.enrollment.domain.model.valueobjects.StudentId;

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

    // public: required by MapStruct, which generates its mapper impl in a different package.
    public Invitation() {
    }

    public static Invitation send(SendInvitationCommand command, StudentId studentId) {
        var invitation = new Invitation();
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
        requirePending(Instant.now());
        status = InvitationStatus.CANCELED;
        respondedAt = Optional.of(Instant.now());
    }

    public void resend(ResendInvitationCommand command) {
        if (status != InvitationStatus.CANCELED && status != InvitationStatus.EXPIRED) {
            throw new InvitationNotResendableException();
        }
        status = InvitationStatus.PENDING;
        validity = InvitationValidity.threeDaysFrom(Instant.now());
        respondedAt = Optional.empty();
    }

    public void accept(AcceptInvitationCommand command) {
        var now = Instant.now();
        requirePending(now);
        status = InvitationStatus.ACCEPTED;
        respondedAt = Optional.of(now);
    }

    public void reject(RejectInvitationCommand command) {
        var now = Instant.now();
        requirePending(now);
        status = InvitationStatus.REJECTED;
        respondedAt = Optional.of(now);
    }

    public void expire(Instant now) {
        if (status == InvitationStatus.PENDING && validity.hasExpired(now)) {
            status = InvitationStatus.EXPIRED;
        }
    }

    // an invitation past its validity is no longer pending, even before the expiration job marks it.
    private void requirePending(Instant now) {
        if (status != InvitationStatus.PENDING || validity.hasExpired(now)) {
            throw new InvitationNotPendingException();
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

    public void setId(InvitationId id) {
        this.id = id;
    }

    public void setCourseId(CourseId courseId) {
        this.courseId = courseId;
    }

    public void setHolderId(String holderId) {
        this.holderId = holderId;
    }

    public void setStudentId(StudentId studentId) {
        this.studentId = studentId;
    }

    public void setInvitedEmail(Email invitedEmail) {
        this.invitedEmail = invitedEmail;
    }

    public void setStatus(InvitationStatus status) {
        this.status = status;
    }

    public void setValidity(InvitationValidity validity) {
        this.validity = validity;
    }

    public void setRespondedAt(Optional<Instant> respondedAt) {
        this.respondedAt = respondedAt;
    }
}
