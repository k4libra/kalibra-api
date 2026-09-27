// InvitationAccepted.java
package com.kalibra.api.enrollment.domain.model.events;

import java.util.UUID;

public record InvitationAccepted(
        UUID invitationId,
        UUID courseId,
        UUID studentId,
        String studentEmail
) {}