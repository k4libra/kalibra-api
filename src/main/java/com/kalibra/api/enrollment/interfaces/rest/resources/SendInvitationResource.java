package com.kalibra.api.enrollment.interfaces.rest.resources;

import java.util.UUID;

public record SendInvitationResource(
        UUID courseId,
        String studentEmail
) {
}