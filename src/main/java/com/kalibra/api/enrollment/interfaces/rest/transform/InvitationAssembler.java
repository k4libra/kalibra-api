package com.kalibra.api.enrollment.interfaces.rest.transform;

import com.kalibra.api.enrollment.domain.model.aggregates.Invitation;
import com.kalibra.api.enrollment.domain.model.valueobjects.CourseInvitationsGroup;
import com.kalibra.api.enrollment.domain.model.valueobjects.PendingInvitationView;
import com.kalibra.api.enrollment.interfaces.rest.resources.CourseInvitationGroupResource;
import com.kalibra.api.enrollment.interfaces.rest.resources.InvitationResource;
import com.kalibra.api.enrollment.interfaces.rest.resources.PendingInvitationResource;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface InvitationAssembler {

    @Mapping(target = "id", expression = "java(invitation.getId().value())")
    @Mapping(target = "courseId", expression = "java(invitation.getCourseId().value())")
    @Mapping(target = "invitedEmail", expression = "java(invitation.getInvitedEmail().value())")
    @Mapping(target = "status", expression = "java(invitation.getStatus().name())")
    @Mapping(target = "sentAt", expression = "java(invitation.getValidity().sentAt())")
    @Mapping(target = "expiresAt", expression = "java(invitation.getValidity().expiresAt())")
    InvitationResource toResource(Invitation invitation);

    default PendingInvitationResource toResource(PendingInvitationView invitation) {
        return new PendingInvitationResource(
                invitation.invitationId(),
                invitation.courseName(),
                invitation.teacherEmail(),
                invitation.sentAt(),
                invitation.expiresAt()
        );
    }

    default CourseInvitationGroupResource toResource(CourseInvitationsGroup group) {
        return new CourseInvitationGroupResource(
                group.courseId(),
                group.courseName(),
                group.courseCode(),
                group.invitations().stream()
                        .map(invitation -> new InvitationResource(
                                invitation.invitationId(),
                                group.courseId(),
                                invitation.invitedEmail(),
                                invitation.status().name(),
                                null,
                                invitation.expiresAt()
                        ))
                        .toList()
        );
    }
}