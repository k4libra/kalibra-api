package com.kalibra.api.progress.domain.model.valueobjects;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProgressValueObjectsTest {

    @Test
    void shouldReportHowManyPointsTheMasteryWentUpOrDown() {
        var up = new MasteryChange(Optional.of(new MasteryProbability(0.30)), new MasteryProbability(0.52));
        var down = new MasteryChange(Optional.of(new MasteryProbability(0.52)), new MasteryProbability(0.41));

        assertThat(up.deltaPoints()).isEqualTo(22);
        assertThat(up.hasChanged()).isTrue();
        assertThat(down.deltaPoints()).isEqualTo(-11);
        assertThat(down.hasChanged()).isTrue();
    }

    @Test
    void shouldReportThatTheMasteryDidNotChange() {
        var same = new MasteryChange(Optional.of(new MasteryProbability(0.951)), new MasteryProbability(0.954));

        assertThat(same.deltaPoints()).isZero();
        assertThat(same.hasChanged()).isFalse();
    }

    @Test
    void shouldTreatAChangeWithoutPreviousValueAsTheFirstEstimate() {
        var first = new MasteryChange(Optional.empty(), new MasteryProbability(0.52));

        assertThat(first.deltaPoints()).isZero();
        assertThat(first.hasChanged()).isTrue();
    }

    @Test
    void shouldExpressAProbabilityAsAPercentageAndRejectValuesOutsideTheUnitInterval() {
        assertThat(new MasteryProbability(0.555).asPercentage()).isEqualTo(56);
        assertThatThrownBy(() -> new MasteryProbability(1.01)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new MasteryProbability(-0.01)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldClassifyLevelsAsLowBelowFortyAndHighAboveSeventy() {
        assertThat(MasteryLevel.ofPercentage(39.9)).isEqualTo(MasteryLevel.LOW);
        assertThat(MasteryLevel.ofPercentage(40)).isEqualTo(MasteryLevel.MEDIUM);
        assertThat(MasteryLevel.ofPercentage(70)).isEqualTo(MasteryLevel.MEDIUM);
        assertThat(MasteryLevel.ofPercentage(70.1)).isEqualTo(MasteryLevel.HIGH);
    }

    @Test
    void shouldCompareTheSelectedOptionIgnoringCaseAndSpaces() {
        var key = new AnswerKey("Statement", "B", "Explanation", new SubtopicId(UUID.randomUUID()));

        assertThat(key.isCorrect("b ")).isTrue();
        assertThat(key.isCorrect("C")).isFalse();
        assertThat(key.isCorrect(null)).isFalse();
    }

    @Test
    void shouldReadTheHistoryFilterDefaultingToAll() {
        assertThat(AttemptResultFilter.from(null)).isEqualTo(AttemptResultFilter.ALL);
        assertThat(AttemptResultFilter.from(" ")).isEqualTo(AttemptResultFilter.ALL);
        assertThat(AttemptResultFilter.from("correct")).isEqualTo(AttemptResultFilter.CORRECT);
        assertThat(AttemptResultFilter.from("INCORRECT")).isEqualTo(AttemptResultFilter.INCORRECT);
        assertThatThrownBy(() -> AttemptResultFilter.from("MAYBE")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldBoundThePagination() {
        assertThat(Pagination.of(0, 100).size()).isEqualTo(100);
        assertThatThrownBy(() -> Pagination.of(-1, 20)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Pagination.of(0, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Pagination.of(0, 101)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldCarryTheBasePriorOfTheEngineOnlyWhenGiven() {
        var plain = new MasteryEstimate(new MasteryProbability(0.5), MasteryLevel.MEDIUM);
        var fromBase = new MasteryEstimate(new MasteryProbability(0.5), MasteryLevel.MEDIUM,
                Optional.of(new MasteryProbability(0.3)), true);

        assertThat(plain.prior()).isEmpty();
        assertThat(plain.initializedFromBase()).isFalse();
        assertThat(fromBase.prior()).contains(new MasteryProbability(0.3));
        assertThatThrownBy(() -> new MasteryEstimate(null, MasteryLevel.LOW)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldExplainTheFourIndicatorsWithoutModelJargon() {
        var guide = IndicatorGuide.standard();

        assertThat(guide.entries()).extracting(IndicatorGuideEntry::indicator)
                .containsExactly(IndicatorKind.ACCURACY, IndicatorKind.PRACTICE,
                        IndicatorKind.MASTERY_EVOLUTION, IndicatorKind.VERIFICATION_APPROVAL);
        assertThat(guide.entries()).allSatisfy(entry -> {
            assertThat(entry.whatItMeasures()).isNotBlank();
            assertThat(entry.goodSignal()).isNotBlank();
            assertThat((entry.whatItMeasures() + entry.goodSignal()).toLowerCase())
                    .doesNotContain("bayesian", "knowledge tracing", "bkt", "probability", "p(l", "parameter");
        });
    }
}
