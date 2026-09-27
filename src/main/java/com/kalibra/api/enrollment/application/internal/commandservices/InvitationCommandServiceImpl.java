package com.kalibra.api.enrollment.application.internal.commandservices;

import com.kalibra.api.enrollment.application.internal.outboundservices.acl.ExternalCurriculumService;
import com.kalibra.api.enrollment.application.internal.outboundservices.acl.ExternalIamService;
import com.kalibra.api.enrollment.application.internal.outboundservices.notifications.InvitationNotificationService;
import com.kalibra.api.enrollment.domain.exceptions.CourseNotOwnedByTeacherException;
import com.kalibra.api.enrollment.domain.exceptions.InvitationNotFoundException;
import com.kalibra.api.enrollment.domain.exceptions.StudentAccountNotFoundException;
import com.kalibra.api.enrollment.domain.exceptions.StudentAlreadyEnrolledException;
import com.kalibra.api.enrollment.domain.exceptions.StudentAlreadyInvitedException;
import com.kalibra.api.enrollment.domain.model.aggregates.Invitation;
import com.kalibra.api.enrollment.domain.model.commands.AcceptInvitationCommand;
import com.kalibra.api.enrollment.domain.model.commands.CancelInvitationCommand;
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
import com.kalibra.api.enrollment.domain.model.valueobjects.InvitationId;
import com.kalibra.api.enrollment.domain.model.valueobjects.InvitationStatus;
import com.kalibra.api.enrollment.domain.model.valueobjects.StudentId;
import com.kalibra.api.enrollment.domain.repositories.EnrollmentRepository;
import com.kalibra.api.enrollment.domain.repositories.InvitationRepository;
import com.kalibra.api.enrollment.domain.services.InvitationCommandService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

// @Transactional on every handler: InvitationAccepted is consumed AFTER_COMMIT, and a
// @TransactionalEventListener drops events published outside a transaction.
@Service
public class InvitationCommandServiceImpl implements InvitationCommandService {

    private final InvitationRepository invitationRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ExternalIamService externalIamService;
    private final ExternalCurriculumService externalCurriculumService;
    private final InvitationNotificationService notificationService;
    private final ApplicationEventPublisher eventPublisher;

    public InvitationCommandServiceImpl(InvitationRepository invitationRepository,
                                        EnrollmentRepository enrollmentRepository,
                                        ExternalIamService externalIamService,
                                        ExternalCurriculumService externalCurriculumService,
                                        InvitationNotificationService notificationService,
                                        ApplicationEventPublisher eventPublisher) {
        this.invitationRepository = invitationRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.externalIamService = externalIamService;
        this.externalCurriculumService = externalCurriculumService;
        this.notificationService = notificationService;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public Invitation handle(SendInvitationCommand command) {
        if (!externalCurriculumService.isCourseOwnedBy(command.courseId(), command.holderId())) {
            throw new CourseNotOwnedByTeacherException();
        }
        var studentId = externalIamService.fetchStudentIdByEmail(command.studentEmail())
                .orElseThrow(() -> new StudentAccountNotFoundException(command.studentEmail().value()));
        ensureNotInvitedNorEnrolled(studentId, command.courseId());

        var saved = savePending(Invitation.send(command, studentId));
        notificationService.notifyInvitation(saved);
        eventPublisher.publishEvent(new InvitationSent(
                saved.getId().value(), saved.getCourseId().value(), saved.getStudentId().value()));
        return saved;
    }

    @Override
    @Transactional
    public Invitation handle(CancelInvitationCommand command) {
        var invitation = findOwnedByTeacher(command.invitationId(), command.holderId());
        invitation.cancel(command);
        var saved = invitationRepository.save(invitation);
        eventPublisher.publishEvent(new InvitationCanceled(saved.getId().value()));
        return saved;
    }

    @Override
    @Transactional
    public Invitation handle(ResendInvitationCommand command) {
        var invitation = findOwnedByTeacher(command.invitationId(), command.holderId());
        invitation.resend(command);
        ensureNotInvitedNorEnrolled(invitation.getStudentId(), invitation.getCourseId());

        var saved = savePending(invitation);
        notificationService.notifyInvitation(saved);
        return saved;
    }

    @Override
    @Transactional
    public Invitation handle(AcceptInvitationCommand command) {
        var invitation = findAddressedToStudent(command.invitationId(), command.holderId());
        invitation.accept(command);
        var saved = invitationRepository.save(invitation);
        eventPublisher.publishEvent(new InvitationAccepted(
                saved.getId().value(), saved.getCourseId().value(),
                saved.getStudentId().value(), saved.getInvitedEmail().value()));
        return saved;
    }

    @Override
    @Transactional
    public Invitation handle(RejectInvitationCommand command) {
        var invitation = findAddressedToStudent(command.invitationId(), command.holderId());
        invitation.reject(command);
        var saved = invitationRepository.save(invitation);
        eventPublisher.publishEvent(new InvitationRejected(saved.getId().value()));
        return saved;
    }

    @Override
    @Transactional
    public int handle(ExpirePendingInvitationsCommand command) {
        var invitations = invitationRepository.findAllByStatusAndExpiresAtBefore(
                InvitationStatus.PENDING, command.now());
        for (var invitation : invitations) {
            invitation.expire(command.now());
            var saved = invitationRepository.save(invitation);
            eventPublisher.publishEvent(new InvitationExpired(saved.getId().value()));
        }
        return invitations.size();
    }

    private Invitation findOwnedByTeacher(InvitationId invitationId, String holderId) {
        return invitationRepository.findByIdAndHolderId(invitationId, holderId)
                .orElseThrow(InvitationNotFoundException::new);
    }

    private Invitation findAddressedToStudent(InvitationId invitationId, String holderId) {
        var studentId = new StudentId(UUID.fromString(holderId));
        return invitationRepository.findByIdAndStudentId(invitationId, studentId)
                .orElseThrow(InvitationNotFoundException::new);
    }

    private void ensureNotInvitedNorEnrolled(StudentId studentId, CourseId courseId) {
        if (enrollmentRepository.findByStudentIdAndCourseId(studentId, courseId).isPresent()) {
            throw new StudentAlreadyEnrolledException();
        }
        if (invitationRepository.findByStudentIdAndCourseIdAndStatus(
                studentId, courseId, InvitationStatus.PENDING).isPresent()) {
            throw new StudentAlreadyInvitedException();
        }
    }

    // the unique pending index settles two concurrent invitations to the same student and course.
    private Invitation savePending(Invitation invitation) {
        try {
            return invitationRepository.save(invitation);
        } catch (DataIntegrityViolationException concurrentInvitation) {
            throw new StudentAlreadyInvitedException();
        }
    }
}
