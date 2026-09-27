package com.kalibra.api.enrollment.application.internal.queryservices;

import com.kalibra.api.enrollment.application.internal.outboundservices.acl.ExternalCurriculumService;
import com.kalibra.api.enrollment.application.internal.outboundservices.acl.ExternalIamService;
import com.kalibra.api.enrollment.domain.model.aggregates.Invitation;
import com.kalibra.api.enrollment.domain.model.queries.GetAcceptedInvitationByStudentAndCourseQuery;
import com.kalibra.api.enrollment.domain.model.queries.GetPendingInvitationsByHolderIdQuery;
import com.kalibra.api.enrollment.domain.model.queries.GetSentInvitationsByHolderIdQuery;
import com.kalibra.api.enrollment.domain.model.valueobjects.CourseInvitationsGroup;
import com.kalibra.api.enrollment.domain.model.valueobjects.InvitationLine;
import com.kalibra.api.enrollment.domain.model.valueobjects.InvitationStatus;
import com.kalibra.api.enrollment.domain.model.valueobjects.PendingInvitationView;
import com.kalibra.api.enrollment.domain.model.valueobjects.StudentId;
import com.kalibra.api.enrollment.domain.repositories.InvitationRepository;
import com.kalibra.api.enrollment.domain.services.InvitationQueryService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class InvitationQueryServiceImpl implements InvitationQueryService {

    private final InvitationRepository invitationRepository;
    private final ExternalIamService externalIamService;
    private final ExternalCurriculumService externalCurriculumService;

    public InvitationQueryServiceImpl(InvitationRepository invitationRepository,
                                      ExternalIamService externalIamService,
                                      ExternalCurriculumService externalCurriculumService) {
        this.invitationRepository = invitationRepository;
        this.externalIamService = externalIamService;
        this.externalCurriculumService = externalCurriculumService;
    }

    // Past its validity an invitation can no longer be answered, even before the
    // expiration job marks it, so it is not offered to the student.
    @Override
    public List<PendingInvitationView> handle(GetPendingInvitationsByHolderIdQuery query) {
        var now = Instant.now();
        var studentId = new StudentId(UUID.fromString(query.holderId()));
        return invitationRepository.findAllByStudentIdAndStatus(studentId, InvitationStatus.PENDING).stream()
                .filter(invitation -> !invitation.getValidity().hasExpired(now))
                .flatMap(invitation -> externalCurriculumService.fetchCourseSummary(invitation.getCourseId())
                        .map(course -> new PendingInvitationView(
                                invitation.getId().value(),
                                course.name(),
                                externalIamService.fetchUserEmail(invitation.getHolderId()).orElse(""),
                                invitation.getValidity().sentAt(),
                                invitation.getValidity().expiresAt()))
                        .stream())
                .toList();
    }

    // Every course of the teacher is listed, even without invitations (FR-027).
    @Override
    public List<CourseInvitationsGroup> handle(GetSentInvitationsByHolderIdQuery query) {
        var invitations = invitationRepository.findAllByHolderId(query.holderId());
        return externalCurriculumService.fetchCoursesByHolderId(query.holderId()).stream()
                .map(course -> new CourseInvitationsGroup(
                        course.courseId(),
                        course.name(),
                        course.code(),
                        invitations.stream()
                                .filter(invitation -> invitation.getCourseId().value().equals(course.courseId()))
                                .map(invitation -> new InvitationLine(
                                        invitation.getId().value(),
                                        invitation.getInvitedEmail().value(),
                                        invitation.getStatus(),
                                        invitation.getValidity().sentAt(),
                                        invitation.getValidity().expiresAt()))
                                .toList()))
                .toList();
    }

    @Override
    public Optional<Invitation> handle(GetAcceptedInvitationByStudentAndCourseQuery query) {
        return invitationRepository.findByStudentIdAndCourseIdAndStatus(
                query.studentId(), query.courseId(), InvitationStatus.ACCEPTED);
    }
}
