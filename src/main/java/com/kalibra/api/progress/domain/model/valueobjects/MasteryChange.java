package com.kalibra.api.progress.domain.model.valueobjects;

import java.util.Optional;

public record MasteryChange(
        Optional<MasteryProbability> previous,
        MasteryProbability current
) {

    public int deltaPoints() {
        return current.asPercentage()
                - previous.map(MasteryProbability::asPercentage).orElse(0);
    }

    public boolean hasChanged() {
        return previous
                .map(value -> value.value() != current.value())
                .orElse(true);
    }
}