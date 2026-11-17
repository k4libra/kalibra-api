package com.kalibra.api.curriculum.domain.services;

import com.kalibra.api.curriculum.domain.model.valueobjects.DifficultyLevel;
import com.kalibra.api.curriculum.domain.model.valueobjects.MasteryProbability;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class DifficultyTargetingPolicyTest {

    private final DifficultyTargetingPolicy policy = new DifficultyTargetingPolicy();

    @Test
    void shouldTargetEasyWhenMasteryIsMissing() {
        assertThat(policy.targetFor(null)).isEqualTo(DifficultyLevel.EASY);
    }

    @ParameterizedTest
    @CsvSource({"0,EASY", "0.399999,EASY", "0.40,MEDIUM", "0.50,MEDIUM", "0.70,MEDIUM", "0.700001,HARD", "1,HARD"})
    void shouldTargetTheEngineBandWhenMasteryIsKnown(double mastery, DifficultyLevel expected) {
        assertThat(policy.targetFor(new MasteryProbability(mastery))).isEqualTo(expected);
    }
}
