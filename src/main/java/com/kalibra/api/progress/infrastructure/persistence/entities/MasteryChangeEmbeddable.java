package com.kalibra.api.progress.infrastructure.persistence.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class MasteryChangeEmbeddable {

    @Column(name = "previous_probability")
    private Double previousProbability;

    @Column(name = "current_probability", nullable = false)
    private double currentProbability;

    // public: required by MapStruct, which generates its mapper impl in a different package.
    public MasteryChangeEmbeddable() {
    }

    public Double getPreviousProbability() {
        return previousProbability;
    }

    public void setPreviousProbability(Double previousProbability) {
        this.previousProbability = previousProbability;
    }

    public double getCurrentProbability() {
        return currentProbability;
    }

    public void setCurrentProbability(double currentProbability) {
        this.currentProbability = currentProbability;
    }
}
