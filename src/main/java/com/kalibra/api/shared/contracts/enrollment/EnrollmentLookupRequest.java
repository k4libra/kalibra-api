package com.kalibra.api.shared.contracts.enrollment;

import java.util.UUID;

public record EnrollmentLookupRequest(
        UUID studentId,
        UUID courseId
) {
}