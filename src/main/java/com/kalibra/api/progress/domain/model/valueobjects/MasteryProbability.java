package com.kalibra.api.progress.domain.model.valueobjects;

public record MasteryProbability(double value) {

    public int asPercentage() {
        return (int) Math.round(value * 100);
    }
}