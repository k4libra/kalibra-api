package com.kalibra.api.progress.interfaces.rest.resources;

import com.kalibra.api.progress.domain.model.valueobjects.MasteryLevel;

import java.util.UUID;

public record SubtopicProgressResource(
        UUID subtopicId,
        String subtopicName,
        Integer masteryPercentage,
        MasteryLevel level,
        int solvedCount
) {}