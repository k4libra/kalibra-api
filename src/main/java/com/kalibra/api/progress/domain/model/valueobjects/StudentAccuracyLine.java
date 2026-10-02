package com.kalibra.api.progress.domain.model.valueobjects;

import java.util.Optional;
import java.util.UUID;

public record StudentAccuracyLine(
        UUID studentId,
        String email,
        int correct,
        int submitted,
        Optional<Double> accuracy,
        boolean hasActivity
) {
}
