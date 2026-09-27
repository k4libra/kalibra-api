package com.kalibra.api.enrollment.domain.model.valueobjects;

import java.util.List;
import java.util.UUID;

public record CourseInvitationsGroup(
        UUID courseId,
        String courseName,
        String courseCode,
        List<InvitationLine> invitations
) {
}