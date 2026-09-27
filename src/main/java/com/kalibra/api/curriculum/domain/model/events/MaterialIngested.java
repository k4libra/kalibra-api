package com.kalibra.api.curriculum.domain.model.events;

import java.util.UUID;

public record MaterialIngested(UUID materialId, UUID courseId) { }
