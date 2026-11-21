package com.kalibra.api.progress.domain.model.valueobjects;

public record MasteryProbability(double value) {

    public MasteryProbability {
        if (Double.isNaN(value) || value < 0 || value > 1) {
            throw new IllegalArgumentException("Mastery probability must be between 0 and 1");
        }
    }

    public int asPercentage() {
        return (int) Math.round(value * 100);
    }
}
