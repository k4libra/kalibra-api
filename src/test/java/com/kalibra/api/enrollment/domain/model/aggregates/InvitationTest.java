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
import com.kalibra.api.enrollment.domain.model.valueobjects.InvitationStatus;
import com.kalibra.api.enrollment.domain.model.valueobjects.InvitationValidity;
import com.kalibra.api.enrollment.domain.model.valueobjects.StudentId;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InvitationTest {

    private final StudentId studentId = new StudentId(UUID.randomUUID());
    private final SendInvitationCommand send = new SendInvitationCommand(
            "teacher-1", new CourseId(UUID.randomUUID()), new Email("ana@kalibra.pe"));

    private Invitation pending() {
        return Invitation.send(send, studentId);
    }

    private Invitation pastItsValidity() {
        var invitation = pending();
        invitation.setValidity(InvitationValidity.threeDaysFrom(Instant.now().minus(Duration.ofDays(4))));
        return invitation;
    }

    private CancelInvitationCommand cancel(Invitation invitation) {
        return new CancelInvitationCommand("teacher-1", invitation.getId());
    }

    private ResendInvitationCommand resend(Invitation invitation) {
        return new ResendInvitationCommand("teacher-1", invitation.getId());
    }

    private AcceptInvitationCommand accept(Invitation invitation) {
        return new AcceptInvitationCommand(studentId.value().toString(), invitation.getId());
    }

    private RejectInvitationCommand reject(Invitation invitation) {
        return new RejectInvitationCommand(studentId.value().toString(), invitation.getId());
    }

    @Test
    void shouldSendAPendingInvitationValidForThreeDays() {
        var invitation = pending();

        assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.PENDING);
        assertThat(invitation.getHolderId()).isEqualTo("teacher-1");
        assertThat(invitation.getStudentId()).isEqualTo(studentId);
        assertThat(Duration.between(invitation.getValidity().sentAt(), invitation.getValidity().expiresAt()))
                .isEqualTo(Duration.ofDays(3));
        assertThat(invitation.getRespondedAt()).isEmpty();
    }

    @Test
    void shouldCancelAPendingInvitation() {
        var invitation = pending();

        invitation.cancel(cancel(invitation));

        assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.CANCELED);
        assertThat(invitation.getRespondedAt()).isPresent();
    }

    @Test
    void shouldNotCancelAnAcceptedInvitation() {
        var invitation = pending();
        invitation.accept(accept(invitation));

        assertThatThrownBy(() -> invitation.cancel(cancel(invitation)))
                .isInstanceOf(InvitationNotPendingException.class);
    }

    @Test
    void shouldResendACanceledInvitationAsPendingWithANewValidity() {
        var invitation = pending();
        invitation.setValidity(InvitationValidity.threeDaysFrom(Instant.now().minus(Duration.ofDays(1))));
        invitation.cancel(cancel(invitation));
        var previousExpiry = invitation.getValidity().expiresAt();

        invitation.resend(resend(invitation));

        assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.PENDING);
        assertThat(invitation.getValidity().expiresAt()).isAfter(previousExpiry);
        assertThat(invitation.getRespondedAt()).isEmpty();
    }

    @Test
    void shouldResendAnExpiredInvitation() {
        var invitation = pastItsValidity();
        invitation.expire(Instant.now());

        invitation.resend(resend(invitation));

        assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.PENDING);
        assertThat(invitation.getValidity().hasExpired(Instant.now())).isFalse();
    }

    @Test
    void shouldNotResendAPendingInvitation() {
        var invitation = pending();

        assertThatThrownBy(() -> invitation.resend(resend(invitation)))
                .isInstanceOf(InvitationNotResendableException.class);
    }

    @Test
    void shouldNotResendAnAcceptedInvitation() {
        var invitation = pending();
        invitation.accept(accept(invitation));

        assertThatThrownBy(() -> invitation.resend(resend(invitation)))
                .isInstanceOf(InvitationNotResendableException.class);
    }

    @Test
    void shouldAcceptAPendingInvitation() {
        var invitation = pending();

        invitation.accept(accept(invitation));

        assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.ACCEPTED);
        assertThat(invitation.getRespondedAt()).isPresent();
    }

    @Test
    void shouldRejectAPendingInvitation() {
        var invitation = pending();

        invitation.reject(reject(invitation));

        assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.REJECTED);
    }

    @Test
    void shouldNotAcceptAnInvitationPastItsValidityBeforeTheJobMarksIt() {
        var invitation = pastItsValidity();

        assertThatThrownBy(() -> invitation.accept(accept(invitation)))
                .isInstanceOf(InvitationNotPendingException.class);
        assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.PENDING);
    }

    @Test
    void shouldNotRejectAnExpiredInvitation() {
        var invitation = pastItsValidity();
        invitation.expire(Instant.now());

        assertThatThrownBy(() -> invitation.reject(reject(invitation)))
                .isInstanceOf(InvitationNotPendingException.class);
    }

    @Test
    void shouldNotAcceptARejectedInvitation() {
        var invitation = pending();
        invitation.reject(reject(invitation));

        assertThatThrownBy(() -> invitation.accept(accept(invitation)))
                .isInstanceOf(InvitationNotPendingException.class);
    }

    @Test
    void shouldExpireAPendingInvitationPastItsValidity() {
        var invitation = pastItsValidity();

        invitation.expire(Instant.now());

        assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.EXPIRED);
    }

    @Test
    void shouldNotExpireAPendingInvitationStillValid() {
        var invitation = pending();

        invitation.expire(Instant.now());

        assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.PENDING);
    }

    @Test
    void shouldNotExpireAnAnsweredInvitation() {
        var invitation = pending();
        invitation.accept(accept(invitation));

        invitation.expire(Instant.now().plus(Duration.ofDays(4)));

        assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.ACCEPTED);
    }
}
