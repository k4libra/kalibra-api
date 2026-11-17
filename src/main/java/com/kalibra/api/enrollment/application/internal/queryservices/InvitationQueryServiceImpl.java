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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        if (query.status() != InvitationStatus.PENDING && query.status() != InvitationStatus.EXPIRED) {
            throw new IllegalArgumentException("Only PENDING and EXPIRED invitations can be listed");
        }
        var invitations = new ArrayList<>(invitationRepository.findAllByStudentIdAndStatus(studentId, InvitationStatus.PENDING));
        if (query.status() == InvitationStatus.EXPIRED) {
            invitations.addAll(invitationRepository.findAllByStudentIdAndStatus(studentId, InvitationStatus.EXPIRED));
        }
        return invitations.stream()
                .filter(invitation -> query.status() == InvitationStatus.EXPIRED
                        ? invitation.getStatus() == InvitationStatus.EXPIRED || invitation.getValidity().hasExpired(now)
                        : !invitation.getValidity().hasExpired(now))
                .flatMap(invitation -> externalCurriculumService.fetchCourseSummary(invitation.getCourseId())
                        .map(course -> new PendingInvitationView(
                                invitation.getId().value(),
                                course.name(),
                                externalIamService.fetchUserEmail(invitation.getHolderId()).orElse(""),
                                invitation.getValidity().sentAt(),
                                course.courseId(), teacherNameOf(invitation.getHolderId()), query.status().name(),
                                invitation.getValidity().expiresAt()))
                        .stream())
                .toList();
    }

    private String teacherNameOf(String holderId) {
        try {
            var name = externalIamService.fetchUserName(UUID.fromString(holderId));
            if (name == null) {
                return null;
            }
            var fullName = Stream.of(name.firstName(), name.lastName())
                    .filter(Objects::nonNull).filter(part -> !part.isBlank())
                    .collect(Collectors.joining(" "));
            return fullName.isBlank() ? null : fullName;
        } catch (IllegalArgumentException malformedHolderId) {
            return null;
        }
    }

    // Every course of the teacher is listed, even without invitations (FR-027); the optional
    // status filters the invitations inside each course, never the courses themselves.
    @Override
    public List<CourseInvitationsGroup> handle(GetSentInvitationsByHolderIdQuery query) {
        var invitations = invitationRepository.findAllByHolderId(query.holderId()).stream()
                .filter(invitation -> query.status().map(invitation.getStatus()::equals).orElse(true))
                .toList();
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
