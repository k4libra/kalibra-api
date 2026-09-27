package com.kalibra.api.enrollment.domain.model.commands;

import com.kalibra.api.enrollment.domain.model.valueobjects.CourseId;
import com.kalibra.api.enrollment.domain.model.valueobjects.Email;

public record SendInvitationCommand(
        String holderId,
        CourseId courseId,
        Email studentEmail
) {
}