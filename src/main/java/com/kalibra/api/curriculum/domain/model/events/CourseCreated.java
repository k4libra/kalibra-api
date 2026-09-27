package com.kalibra.api.curriculum.domain.model.events;

import java.util.UUID;

public record CourseCreated(UUID courseId, String holderId) { }
