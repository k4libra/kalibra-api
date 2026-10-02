package com.kalibra.api.curriculum.domain.model.valueobjects;

public record MasteryProbability(double value) {

    public MasteryProbability {
        if (Double.isNaN(value) || value < 0 || value > 1) {
            throw new IllegalArgumentException("Mastery probability must be between 0 and 1");
        }
    }
}
