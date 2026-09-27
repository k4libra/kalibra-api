package com.kalibra.api.enrollment.application.internal.commandservices;

import com.kalibra.api.enrollment.application.internal.outboundservices.acl.ExternalCurriculumService;
import com.kalibra.api.enrollment.application.internal.outboundservices.acl.ExternalIamService;
import com.kalibra.api.enrollment.application.internal.outboundservices.notifications.InvitationNotificationService;
import com.kalibra.api.enrollment.domain.exceptions.CourseNotOwnedByTeacherException;
import com.kalibra.api.enrollment.domain.exceptions.InvitationNotFoundException;
import com.kalibra.api.enrollment.domain.exceptions.StudentAccountNotFoundException;
import com.kalibra.api.enrollment.domain.model.aggregates.Invitation;
import com.kalibra.api.enrollment.domain.model.commands.*;
import com.kalibra.api.enrollment.domain.model.events.*;
import com.kalibra.api.enrollment.domain.model.valueobjects.InvitationStatus;
import com.kalibra.api.enrollment.domain.repositories.InvitationRepository;
import com.kalibra.api.enrollment.domain.services.InvitationCommandService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class InvitationCommandServiceImpl implements InvitationCommandService {

    private final InvitationRepository invitationRepository;
    private final ExternalIamService externalIamService;
    private final ExternalCurriculumService externalCurriculumService;
    private final InvitationNotificationService notificationService;
    private final ApplicationEventPublisher eventPublisher;

    public InvitationCommandServiceImpl(
            InvitationRepository invitationRepository,
            ExternalIamService externalIamService,
            ExternalCurriculumService externalCurriculumService,
            InvitationNotificationService notificationService,
            ApplicationEventPublisher eventPublisher
    ) {
        this.invitationRepository = invitationRepository;
        this.externalIamService = externalIamService;
        this.externalCurriculumService = externalCurriculumService;
        this.notificationService = notificationService;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Invitation handle(SendInvitationCommand command) {

        if (!externalCurriculumService.isCourseOwnedBy(
                command.courseId(),
                command.holderId()
        )) {
            throw new CourseNotOwnedByTeacherException();
        }

        var studentId = externalIamService
                .fetchStudentIdByEmail(command.studentEmail())
                .orElseThrow(() ->
                        new StudentAccountNotFoundException(
                                command.studentEmail().value()
                        )
                );

        var invitation = Invitation.send(command, studentId);
        var saved = invitationRepository.save(invitation);

        notificationService.notifyInvitation(saved);

        eventPublisher.publishEvent(
                new InvitationSent(
                        saved.getId().value(),
                        saved.getCourseId().value(),
                        saved.getStudentId().value()
                )
        );

        return saved;
    }

    @Override
    public Invitation handle(CancelInvitationCommand command) {

        var invitation = invitationRepository
                .findByIdAndHolderId(
                        command.invitationId(),
                        command.holderId()
                )
                .orElseThrow(InvitationNotFoundException::new);

        invitation.cancel(command);

        var saved = invitationRepository.save(invitation);

        eventPublisher.publishEvent(
                new InvitationCanceled(saved.getId().value())
        );

        return saved;
    }

    @Override
    public Invitation handle(ResendInvitationCommand command) {

        var invitation = invitationRepository
                .findByIdAndHolderId(
                        command.invitationId(),
                        command.holderId()
                )
                .orElseThrow(InvitationNotFoundException::new);

        invitation.resend(command);

        var saved = invitationRepository.save(invitation);

        notificationService.notifyInvitation(saved);

        return saved;
    }

    @Override
    public Invitation handle(AcceptInvitationCommand command) {

        var invitation = invitationRepository
                .findByIdAndStudentId(
                        command.invitationId(),
                        new com.kalibra.api.enrollment.domain.model.valueobjects.StudentId(
                                java.util.UUID.fromString(command.holderId())
                        )
                )
                .orElseThrow(InvitationNotFoundException::new);

        invitation.accept(command);

        var saved = invitationRepository.save(invitation);

        eventPublisher.publishEvent(
                new InvitationAccepted(
                        saved.getId().value(),
                        saved.getCourseId().value(),
                        saved.getStudentId().value(),
                        saved.getInvitedEmail().value()
                )
        );

        return saved;
    }

    @Override
    public Invitation handle(RejectInvitationCommand command) {

        var invitation = invitationRepository
                .findByIdAndStudentId(
                        command.invitationId(),
                        new com.kalibra.api.enrollment.domain.model.valueobjects.StudentId(
                                java.util.UUID.fromString(command.holderId())
                        )
                )
                .orElseThrow(InvitationNotFoundException::new);

        invitation.reject(command);

        var saved = invitationRepository.save(invitation);

        eventPublisher.publishEvent(
                new InvitationRejected(saved.getId().value())
        );

        return saved;
    }

    @Override
    public int handle(ExpirePendingInvitationsCommand command) {

        var invitations =
                invitationRepository.findAllByStatusAndExpiresAtBefore(
                        InvitationStatus.PENDING,
                        command.now()
                );

        for (var invitation : invitations) {
            invitation.expire(command.now());

            var saved = invitationRepository.save(invitation);

            eventPublisher.publishEvent(
                    new InvitationExpired(saved.getId().value())
            );
        }

        return invitations.size();
    }
}