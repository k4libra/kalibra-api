package com.kalibra.api.progress.domain.model.valueobjects;

import java.util.Optional;

public record MasteryEstimate(
        MasteryProbability probability,
        MasteryLevel level,
        Optional<MasteryProbability> prior,
        boolean initializedFromBase
) {

    public MasteryEstimate {
        if (probability == null || level == null) {
            throw new IllegalArgumentException("A mastery estimate needs its probability and level");
        }
        if (prior == null) {
            prior = Optional.empty();
        }
    }

    public MasteryEstimate(MasteryProbability probability, MasteryLevel level) {
        this(probability, level, Optional.empty(), false);
    }
}
