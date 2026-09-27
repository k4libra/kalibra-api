package com.kalibra.api.enrollment.interfaces.rest.transform;

import com.kalibra.api.enrollment.domain.model.aggregates.Invitation;
import com.kalibra.api.enrollment.interfaces.rest.resources.InvitationResource;
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
}