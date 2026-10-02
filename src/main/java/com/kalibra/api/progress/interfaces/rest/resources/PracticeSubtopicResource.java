package com.kalibra.api.progress.interfaces.rest.resources;

import com.kalibra.api.progress.domain.model.valueobjects.MasteryLevel;

import java.util.UUID;

public record PracticeSubtopicResource(
        UUID subtopicId,
        String name,
        MasteryLevel level
) {}