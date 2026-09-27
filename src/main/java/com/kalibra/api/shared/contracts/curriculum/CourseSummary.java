package com.kalibra.api.shared.contracts.curriculum;

import java.util.UUID;

public record CourseSummary(UUID courseId, String name, String code, String holderId) { }
