package com.kalibra.api.enrollment.interfaces.rest.resources;

import java.util.List;
import java.util.UUID;

public record CourseInvitationGroupResource(
        UUID courseId,
        String courseName,
        String courseCode,
        List<InvitationResource> invitations
) {
}