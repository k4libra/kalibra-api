package com.kalibra.api.progress.domain.model.valueobjects;

import java.util.Optional;
import java.util.UUID;

public record SubtopicEvolutionLine(
        UUID subtopicId,
        String subtopicName,
        Optional<Double> initialAverage,
        Optional<Double> currentAverage,
        Optional<Integer> deltaPoints,
        MasteryLevel level,
        boolean practiced
) {
}
