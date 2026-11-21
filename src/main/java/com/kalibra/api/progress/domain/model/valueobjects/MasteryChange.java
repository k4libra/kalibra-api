package com.kalibra.api.progress.domain.model.valueobjects;

import java.util.Optional;

public record MasteryChange(
        Optional<MasteryProbability> previous,
        MasteryProbability current
) {

    public MasteryChange {
        if (current == null) {
            throw new IllegalArgumentException("A mastery change needs the current probability");
        }
        if (previous == null) {
            previous = Optional.empty();
        }
    }

    public int deltaPoints() {
        return previous
                .map(value -> current.asPercentage() - value.asPercentage())
                .orElse(0);
    }

    public boolean hasChanged() {
        return previous.isEmpty() || deltaPoints() != 0;
    }
}
