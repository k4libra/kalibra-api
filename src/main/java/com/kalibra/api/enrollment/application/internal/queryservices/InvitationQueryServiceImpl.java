package com.kalibra.api.enrollment.application.internal.queryservices;

import com.kalibra.api.enrollment.application.internal.outboundservices.acl.ExternalCurriculumService;
import com.kalibra.api.enrollment.application.internal.outboundservices.acl.ExternalIamService;
import com.kalibra.api.enrollment.domain.model.aggregates.Invitation;
import com.kalibra.api.enrollment.domain.model.queries.GetAcceptedInvitationByStudentAndCourseQuery;
import com.kalibra.api.enrollment.domain.model.queries.GetPendingInvitationsByHolderIdQuery;
import com.kalibra.api.enrollment.domain.model.queries.GetSentInvitationsByHolderIdQuery;
import com.kalibra.api.enrollment.domain.model.valueobjects.*;
import com.kalibra.api.enrollment.domain.repositories.InvitationRepository;
import com.kalibra.api.enrollment.domain.services.InvitationQueryService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class InvitationQueryServiceImpl implements InvitationQueryService {

    private final InvitationRepository invitationRepository;
    private final ExternalIamService externalIamService;
    private final ExternalCurriculumService externalCurriculumService;

    public InvitationQueryServiceImpl(
            InvitationRepository invitationRepository,
            ExternalIamService externalIamService,
            ExternalCurriculumService externalCurriculumService
    ) {
        this.invitationRepository = invitationRepository;
        this.externalIamService = externalIamService;
        this.externalCurriculumService = externalCurriculumService;
    }

    @Override
    public List<PendingInvitationView> handle(
            GetPendingInvitationsByHolderIdQuery query
    ) {
        var studentId = new StudentId(UUID.fromString(query.holderId()));

        var invitations = invitationRepository.findAllByStudentIdAndStatus(
                studentId,
                InvitationStatus.PENDING
        );

        var result = new ArrayList<PendingInvitationView>();

        for (var invitation : invitations) {
            var course = externalCurriculumService
                    .fetchCourseSummary(invitation.getCourseId());

            var teacherEmail = externalIamService
                    .fetchUserEmail(invitation.getHolderId());

            if (course.isPresent()) {
                result.add(new PendingInvitationView(
                        invitation.getId().value(),
                        course.get().name(),
                        teacherEmail.orElse(""),
                        invitation.getValidity().sentAt(),
                        invitation.getValidity().expiresAt()
                ));
            }
        }

        return result;
    }

    @Override
    public List<CourseInvitationsGroup> handle(
            GetSentInvitationsByHolderIdQuery query
    ) {
        var invitations = invitationRepository.findAllByHolderId(
                query.holderId()
        );

        var courses = externalCurriculumService.fetchCoursesByHolderId(
                query.holderId()
        );

        var result = new ArrayList<CourseInvitationsGroup>();

        for (var course : courses) {

            var lines = invitations.stream()
                    .filter(invitation ->
                            invitation.getCourseId().value()
                                    .equals(course.courseId()))
                    .map(invitation -> new InvitationLine(
                            invitation.getId().value(),
                            invitation.getInvitedEmail().value(),
                            invitation.getStatus(),
                            invitation.getValidity().expiresAt()
                    ))
                    .toList();

            result.add(new CourseInvitationsGroup(
                    course.courseId(),
                    course.name(),
                    course.code(),
                    lines
            ));
        }

        return result;
    }

    @Override
    public Optional<Invitation> handle(
            GetAcceptedInvitationByStudentAndCourseQuery query
    ) {
        return invitationRepository.findByStudentIdAndCourseIdAndStatus(
                query.studentId(),
                query.courseId(),
                InvitationStatus.ACCEPTED
        );
    }
}