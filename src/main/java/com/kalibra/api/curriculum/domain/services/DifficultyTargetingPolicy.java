package com.kalibra.api.curriculum.domain.services;

import com.kalibra.api.curriculum.domain.model.valueobjects.DifficultyLevel;
import com.kalibra.api.curriculum.domain.model.valueobjects.MasteryProbability;

public class DifficultyTargetingPolicy {

    public DifficultyLevel targetFor(MasteryProbability mastery) {
        if (mastery == null || mastery.value() < 0.40) {
            return DifficultyLevel.EASY;
        }
        if (mastery.value() <= 0.70) {
            return DifficultyLevel.MEDIUM;
        }
        return DifficultyLevel.HARD;
    }
}
