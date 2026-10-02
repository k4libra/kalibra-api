package com.kalibra.api.progress.domain.model.valueobjects;

public record MasteryEstimate(
        MasteryProbability probability,
        MasteryLevel level
) {
}