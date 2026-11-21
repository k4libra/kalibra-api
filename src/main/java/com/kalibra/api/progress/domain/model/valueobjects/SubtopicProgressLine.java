package com.kalibra.api.progress.domain.model.valueobjects;

import java.util.Optional;
import java.util.UUID;

public record SubtopicProgressLine(
        UUID subtopicId,
        String subtopicName,
        Optional<MasteryProbability> mastery,
        MasteryLevel level,
        int solvedCount
) {
}