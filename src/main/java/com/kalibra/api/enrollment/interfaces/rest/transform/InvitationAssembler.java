package com.kalibra.api.enrollment.interfaces.rest.transform;

import com.kalibra.api.enrollment.domain.model.aggregates.Invitation;
import com.kalibra.api.enrollment.domain.model.commands.SendInvitationCommand;
import com.kalibra.api.enrollment.domain.model.valueobjects.CourseId;
import com.kalibra.api.enrollment.domain.model.valueobjects.CourseInvitationsGroup;
import com.kalibra.api.enrollment.domain.model.valueobjects.Email;
import com.kalibra.api.enrollment.domain.model.valueobjects.PendingInvitationView;
import com.kalibra.api.enrollment.interfaces.rest.resources.CourseInvitationGroupResource;
import com.kalibra.api.enrollment.interfaces.rest.resources.InvitationResource;
import com.kalibra.api.enrollment.interfaces.rest.resources.PendingInvitationResource;
import com.kalibra.api.enrollment.interfaces.rest.resources.SendInvitationResource;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface InvitationAssembler {

    default SendInvitationCommand toCommand(SendInvitationResource resource, String holderId) {
        return new SendInvitationCommand(
                holderId, new CourseId(resource.courseId()), new Email(resource.studentEmail().trim()));
    }

    @Mapping(target = "id", expression = "java(invitation.getId().value())")
    @Mapping(target = "courseId", expression = "java(invitation.getCourseId().value())")
    @Mapping(target = "invitedEmail", expression = "java(invitation.getInvitedEmail().value())")
    @Mapping(target = "status", expression = "java(invitation.getStatus().name())")
    @Mapping(target = "sentAt", expression = "java(invitation.getValidity().sentAt())")
    @Mapping(target = "expiresAt", expression = "java(invitation.getValidity().expiresAt())")
    InvitationResource toResource(Invitation invitation);

    @Mapping(target = "id", source = "invitationId")
    PendingInvitationResource toResource(PendingInvitationView invitation);

    default CourseInvitationGroupResource toResource(CourseInvitationsGroup group) {
        return new CourseInvitationGroupResource(
                group.courseId(),
                group.courseName(),
                group.courseCode(),
                group.invitations().stream()
                        .map(line -> new InvitationResource(
                                line.invitationId(),
                                group.courseId(),
                                line.invitedEmail(),
                                line.status().name(),
                                line.sentAt(),
                                line.expiresAt()))
                        .toList());
    }
}
