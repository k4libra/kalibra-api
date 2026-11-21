package com.kalibra.api.curriculum.domain.model.valueobjects;

import java.util.UUID;

public record SubtopicExerciseCount(UUID subtopicId, String subtopicName, int generated, int approved, int discarded) {

    public double approvalRate() {
        return generated == 0 ? 0 : approved * 100.0 / generated;
    }
}
