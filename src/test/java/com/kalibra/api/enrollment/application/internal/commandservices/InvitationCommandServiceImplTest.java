package com.kalibra.api.enrollment.application.internal.commandservices;

import com.kalibra.api.enrollment.application.internal.outboundservices.acl.ExternalCurriculumService;
import com.kalibra.api.enrollment.application.internal.outboundservices.acl.ExternalIamService;
import com.kalibra.api.enrollment.application.internal.outboundservices.notifications.InvitationNotificationService;
import com.kalibra.api.enrollment.domain.exceptions.CourseNotOwnedByTeacherException;
import com.kalibra.api.enrollment.domain.exceptions.InvitationNotFoundException;
import com.kalibra.api.enrollment.domain.exceptions.InvitationNotResendableException;
import com.kalibra.api.enrollment.domain.exceptions.StudentAccountNotFoundException;
import com.kalibra.api.enrollment.domain.exceptions.StudentAlreadyEnrolledException;
import com.kalibra.api.enrollment.domain.exceptions.StudentAlreadyInvitedException;
import com.kalibra.api.enrollment.domain.model.aggregates.Enrollment;
import com.kalibra.api.enrollment.domain.model.aggregates.Invitation;
import com.kalibra.api.enrollment.domain.model.commands.AcceptInvitationCommand;
import com.kalibra.api.enrollment.domain.model.commands.CancelInvitationCommand;
import com.kalibra.api.enrollment.domain.model.commands.EnrollStudentCommand;
import com.kalibra.api.enrollment.domain.model.commands.ExpirePendingInvitationsCommand;
import com.kalibra.api.enrollment.domain.model.commands.RejectInvitationCommand;
import com.kalibra.api.enrollment.domain.model.commands.ResendInvitationCommand;
import com.kalibra.api.enrollment.domain.model.commands.SendInvitationCommand;
import com.kalibra.api.enrollment.domain.model.events.InvitationAccepted;
import com.kalibra.api.enrollment.domain.model.events.InvitationCanceled;
import com.kalibra.api.enrollment.domain.model.events.InvitationExpired;
import com.kalibra.api.enrollment.domain.model.events.InvitationRejected;
import com.kalibra.api.enrollment.domain.model.events.InvitationSent;
import com.kalibra.api.enrollment.domain.model.valueobjects.CourseId;
import com.kalibra.api.enrollment.domain.model.valueobjects.Email;
import com.kalibra.api.enrollment.domain.model.valueobjects.InvitationId;
import com.kalibra.api.enrollment.domain.model.valueobjects.InvitationStatus;
import com.kalibra.api.enrollment.domain.model.valueobjects.InvitationValidity;
import com.kalibra.api.enrollment.domain.model.valueobjects.StudentId;
import com.kalibra.api.enrollment.domain.repositories.EnrollmentRepository;
import com.kalibra.api.enrollment.domain.repositories.InvitationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvitationCommandServiceImplTest {

    @Mock
    InvitationRepository invitationRepository;

    @Mock
    EnrollmentRepository enrollmentRepository;

    @Mock
    ExternalIamService externalIamService;

    @Mock
    ExternalCurriculumService externalCurriculumService;

    @Mock
    InvitationNotificationService notificationService;

    @Mock
    ApplicationEventPublisher eventPublisher;

    @InjectMocks
    InvitationCommandServiceImpl service;

    private final CourseId courseId = new CourseId(UUID.randomUUID());
    private final StudentId studentId = new StudentId(UUID.randomUUID());
    private final Email email = new Email("ana@kalibra.pe");
    private final SendInvitationCommand send = new SendInvitationCommand("teacher-1", courseId, email);

    private Invitation pending() {
        return Invitation.send(send, studentId);
    }

    private void savesReturnTheInvitation() {
        when(invitationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void shouldSendAndNotifyAnInvitationToARegisteredStudent() {
        // Arrange
        when(externalCurriculumService.isCourseOwnedBy(courseId, "teacher-1")).thenReturn(true);
        when(externalIamService.fetchStudentIdByEmail(email)).thenReturn(Optional.of(studentId));
        savesReturnTheInvitation();

        // Act
        var invitation = service.handle(send);

        // Assert
        assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.PENDING);
        verify(notificationService).notifyInvitation(invitation);
        verify(eventPublisher).publishEvent(any(InvitationSent.class));
    }

    @Test
    void shouldNotInviteToACourseOfAnotherTeacher() {
        when(externalCurriculumService.isCourseOwnedBy(courseId, "teacher-1")).thenReturn(false);

        assertThatThrownBy(() -> service.handle(send)).isInstanceOf(CourseNotOwnedByTeacherException.class);
        verify(invitationRepository, never()).save(any());
    }

    @Test
    void shouldNotInviteAnEmailWithoutAStudentAccountNorNotify() {
        when(externalCurriculumService.isCourseOwnedBy(courseId, "teacher-1")).thenReturn(true);
        when(externalIamService.fetchStudentIdByEmail(email)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.handle(send)).isInstanceOf(StudentAccountNotFoundException.class);
        verify(notificationService, never()).notifyInvitation(any());
    }

    @Test
    void shouldNotInviteAStudentWithAPendingInvitationToTheCourse() {
        when(externalCurriculumService.isCourseOwnedBy(courseId, "teacher-1")).thenReturn(true);
        when(externalIamService.fetchStudentIdByEmail(email)).thenReturn(Optional.of(studentId));
        when(invitationRepository.findByStudentIdAndCourseIdAndStatus(studentId, courseId, InvitationStatus.PENDING))
                .thenReturn(Optional.of(pending()));

        assertThatThrownBy(() -> service.handle(send)).isInstanceOf(StudentAlreadyInvitedException.class);
        verify(invitationRepository, never()).save(any());
    }

    @Test
    void shouldNotInviteAStudentAlreadyEnrolledInTheCourse() {
        when(externalCurriculumService.isCourseOwnedBy(courseId, "teacher-1")).thenReturn(true);
        when(externalIamService.fetchStudentIdByEmail(email)).thenReturn(Optional.of(studentId));
        when(enrollmentRepository.findByStudentIdAndCourseId(studentId, courseId))
                .thenReturn(Optional.of(Enrollment.fromAcceptedInvitation(
                        new EnrollStudentCommand(new InvitationId(UUID.randomUUID()), courseId, studentId, email))));

        assertThatThrownBy(() -> service.handle(send)).isInstanceOf(StudentAlreadyEnrolledException.class);
        verify(invitationRepository, never()).save(any());
    }

    @Test
    void shouldAnswerConflictWhenAConcurrentInvitationWinsTheRace() {
        when(externalCurriculumService.isCourseOwnedBy(courseId, "teacher-1")).thenReturn(true);
        when(externalIamService.fetchStudentIdByEmail(email)).thenReturn(Optional.of(studentId));
        when(invitationRepository.save(any())).thenThrow(new DataIntegrityViolationException("uq_invitations_pending"));

        assertThatThrownBy(() -> service.handle(send)).isInstanceOf(StudentAlreadyInvitedException.class);
        verify(notificationService, never()).notifyInvitation(any());
    }

    @Test
    void shouldCancelAnInvitationOfTheTeacher() {
        var invitation = pending();
        when(invitationRepository.findByIdAndHolderId(invitation.getId(), "teacher-1")).thenReturn(Optional.of(invitation));
        savesReturnTheInvitation();

        var canceled = service.handle(new CancelInvitationCommand("teacher-1", invitation.getId()));

        assertThat(canceled.getStatus()).isEqualTo(InvitationStatus.CANCELED);
        verify(eventPublisher).publishEvent(new InvitationCanceled(invitation.getId().value()));
    }

    @Test
    void shouldNotFindAnInvitationOfAnotherTeacher() {
        var invitationId = new InvitationId(UUID.randomUUID());
        when(invitationRepository.findByIdAndHolderId(invitationId, "teacher-2")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.handle(new CancelInvitationCommand("teacher-2", invitationId)))
                .isInstanceOf(InvitationNotFoundException.class);
    }

    @Test
    void shouldResendACanceledInvitationAndNotifyAgain() {
        // Arrange
        var invitation = pending();
        invitation.cancel(new CancelInvitationCommand("teacher-1", invitation.getId()));
        when(invitationRepository.findByIdAndHolderId(invitation.getId(), "teacher-1")).thenReturn(Optional.of(invitation));
        savesReturnTheInvitation();

        // Act
        var resent = service.handle(new ResendInvitationCommand("teacher-1", invitation.getId()));

        // Assert
        assertThat(resent.getStatus()).isEqualTo(InvitationStatus.PENDING);
        verify(notificationService).notifyInvitation(resent);
    }

    @Test
    void shouldNotResendWhenTheStudentWasInvitedAgainMeanwhile() {
        var invitation = pending();
        invitation.cancel(new CancelInvitationCommand("teacher-1", invitation.getId()));
        when(invitationRepository.findByIdAndHolderId(invitation.getId(), "teacher-1")).thenReturn(Optional.of(invitation));
        when(invitationRepository.findByStudentIdAndCourseIdAndStatus(studentId, courseId, InvitationStatus.PENDING))
                .thenReturn(Optional.of(pending()));

        assertThatThrownBy(() -> service.handle(new ResendInvitationCommand("teacher-1", invitation.getId())))
                .isInstanceOf(StudentAlreadyInvitedException.class);
        verify(notificationService, never()).notifyInvitation(any());
    }

    @Test
    void shouldNotResendAPendingInvitation() {
        var invitation = pending();
        when(invitationRepository.findByIdAndHolderId(invitation.getId(), "teacher-1")).thenReturn(Optional.of(invitation));

        assertThatThrownBy(() -> service.handle(new ResendInvitationCommand("teacher-1", invitation.getId())))
                .isInstanceOf(InvitationNotResendableException.class);
    }

    @Test
    void shouldAcceptAnInvitationAddressedToTheStudentAndPublishIt() {
        var invitation = pending();
        when(invitationRepository.findByIdAndStudentId(invitation.getId(), studentId)).thenReturn(Optional.of(invitation));
        savesReturnTheInvitation();

        var accepted = service.handle(new AcceptInvitationCommand(studentId.value().toString(), invitation.getId()));

        assertThat(accepted.getStatus()).isEqualTo(InvitationStatus.ACCEPTED);
        verify(eventPublisher).publishEvent(new InvitationAccepted(
                invitation.getId().value(), courseId.value(), studentId.value(), email.value()));
    }

    @Test
    void shouldNotFindAnInvitationAddressedToAnotherStudent() {
        var invitationId = new InvitationId(UUID.randomUUID());
        var otherStudent = UUID.randomUUID();
        when(invitationRepository.findByIdAndStudentId(invitationId, new StudentId(otherStudent))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.handle(new AcceptInvitationCommand(otherStudent.toString(), invitationId)))
                .isInstanceOf(InvitationNotFoundException.class);
    }

    @Test
    void shouldRejectAnInvitationWithoutEnrolling() {
        var invitation = pending();
        when(invitationRepository.findByIdAndStudentId(invitation.getId(), studentId)).thenReturn(Optional.of(invitation));
        savesReturnTheInvitation();

        var rejected = service.handle(new RejectInvitationCommand(studentId.value().toString(), invitation.getId()));

        assertThat(rejected.getStatus()).isEqualTo(InvitationStatus.REJECTED);
        verify(eventPublisher).publishEvent(new InvitationRejected(invitation.getId().value()));
        verify(eventPublisher, never()).publishEvent(any(InvitationAccepted.class));
    }

    @Test
    void shouldExpireThePendingInvitationsPastTheirValidity() {
        // Arrange
        var now = Instant.now();
        var overdue = pending();
        overdue.setValidity(InvitationValidity.threeDaysFrom(now.minus(Duration.ofDays(4))));
        when(invitationRepository.findAllByStatusAndExpiresAtBefore(InvitationStatus.PENDING, now)).thenReturn(List.of(overdue));
        savesReturnTheInvitation();

        // Act
        var expired = service.handle(new ExpirePendingInvitationsCommand(now));

        // Assert
        assertThat(expired).isEqualTo(1);
        assertThat(overdue.getStatus()).isEqualTo(InvitationStatus.EXPIRED);
        verify(eventPublisher).publishEvent(eq(new InvitationExpired(overdue.getId().value())));
    }
}
