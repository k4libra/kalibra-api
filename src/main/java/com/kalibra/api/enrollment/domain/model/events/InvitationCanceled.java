// InvitationCanceled.java
package com.kalibra.api.enrollment.domain.model.events;

import java.util.UUID;

public record InvitationCanceled(
        UUID invitationId
) {}