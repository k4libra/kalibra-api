package com.kalibra.api.progress.domain.model.aggregates;

import com.kalibra.api.progress.domain.model.commands.UpdateSubtopicMasteryCommand;
import com.kalibra.api.progress.domain.model.valueobjects.CourseId;
import com.kalibra.api.progress.domain.model.valueobjects.MasteryEstimate;
import com.kalibra.api.progress.domain.model.valueobjects.MasteryLevel;
import com.kalibra.api.progress.domain.model.valueobjects.MasteryProbability;
import com.kalibra.api.progress.domain.model.valueobjects.SubtopicId;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SubtopicMasteryTest {

    private final CourseId courseId = new CourseId(UUID.randomUUID());
    private final SubtopicId subtopicId = new SubtopicId(UUID.randomUUID());

    private UpdateSubtopicMasteryCommand command(double probability, MasteryLevel level) {
        return new UpdateSubtopicMasteryCommand("student-1", courseId, subtopicId,
                new MasteryEstimate(new MasteryProbability(probability), level));
    }

    @Test
    void shouldKeepTheFirstEstimateAsTheBaselineOfTheEvolution() {
        var mastery = SubtopicMastery.firstEstimate(command(0.35, MasteryLevel.LOW));

        assertThat(mastery.getInitialEstimate().value()).isEqualTo(0.35);
        assertThat(mastery.getCurrentEstimate().value()).isEqualTo(0.35);
        assertThat(mastery.getLevel()).isEqualTo(MasteryLevel.LOW);
        assertThat(mastery.getEstimatesCount()).isEqualTo(1);
        assertThat(mastery.evolutionPoints()).isZero();
        assertThat(mastery.getHolderId()).isEqualTo("student-1");
    }

    @Test
    void shouldApplyLaterEstimatesWithoutTouchingTheBaseline() {
        var mastery = SubtopicMastery.firstEstimate(command(0.35, MasteryLevel.LOW));

        mastery.apply(command(0.58, MasteryLevel.MEDIUM));
        mastery.apply(command(0.74, MasteryLevel.HIGH));

        assertThat(mastery.getInitialEstimate().value()).isEqualTo(0.35);
        assertThat(mastery.getCurrentEstimate().value()).isEqualTo(0.74);
        assertThat(mastery.getLevel()).isEqualTo(MasteryLevel.HIGH);
        assertThat(mastery.getEstimatesCount()).isEqualTo(3);
        assertThat(mastery.evolutionPoints()).isEqualTo(39);
    }

    @Test
    void shouldReportANegativeEvolutionWhenTheMasteryGoesDown() {
        var mastery = SubtopicMastery.firstEstimate(command(0.50, MasteryLevel.MEDIUM));

        mastery.apply(command(0.31, MasteryLevel.LOW));

        assertThat(mastery.evolutionPoints()).isEqualTo(-19);
    }
}
