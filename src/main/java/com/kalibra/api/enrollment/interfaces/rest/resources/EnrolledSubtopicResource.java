package com.kalibra.api.enrollment.interfaces.rest.resources;

import java.util.UUID;

public record EnrolledSubtopicResource(
        UUID id,
        String name,
        int displayOrder
) {
}
