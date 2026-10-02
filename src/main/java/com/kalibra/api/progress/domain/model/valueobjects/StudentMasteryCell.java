package com.kalibra.api.progress.domain.model.valueobjects;

import java.util.Optional;
import java.util.UUID;

public record StudentMasteryCell(
        UUID studentId,
        String email,
        UUID subtopicId,
        Optional<MasteryProbability> mastery,
        MasteryLevel level
) {
}