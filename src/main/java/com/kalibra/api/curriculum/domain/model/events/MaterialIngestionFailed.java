package com.kalibra.api.curriculum.domain.model.events;

import java.util.UUID;

public record MaterialIngestionFailed(UUID materialId, String reason) { }
