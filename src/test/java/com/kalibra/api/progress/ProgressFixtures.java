package com.kalibra.api.progress;

import com.kalibra.api.progress.domain.model.aggregates.ExerciseAttempt;
import com.kalibra.api.progress.domain.model.aggregates.SubtopicMastery;
import com.kalibra.api.progress.domain.model.commands.SubmitAnswerCommand;
import com.kalibra.api.progress.domain.model.commands.UpdateSubtopicMasteryCommand;
import com.kalibra.api.progress.domain.model.valueobjects.AnswerKey;
import com.kalibra.api.progress.domain.model.valueobjects.CourseId;
import com.kalibra.api.progress.domain.model.valueobjects.ExerciseId;
import com.kalibra.api.progress.domain.model.valueobjects.MasteryChange;
import com.kalibra.api.progress.domain.model.valueobjects.MasteryEstimate;
import com.kalibra.api.progress.domain.model.valueobjects.MasteryLevel;
import com.kalibra.api.progress.domain.model.valueobjects.MasteryProbability;
import com.kalibra.api.progress.domain.model.valueobjects.SubtopicId;

import java.util.Optional;
import java.util.UUID;

public final class ProgressFixtures {

    public static final String EXPLANATION = "Subtract 3, divide by 2";

    private ProgressFixtures() {
    }

    public static AnswerKey answerKey(SubtopicId subtopicId) {
        return new AnswerKey("Solve 2x + 3 = 7", "B", EXPLANATION, subtopicId);
    }

    public static ExerciseAttempt attempt(String holderId, CourseId courseId, SubtopicId subtopicId,
                                          String selectedOptionKey, Double previous, double current) {
        var command = new SubmitAnswerCommand(holderId, courseId, new ExerciseId(UUID.randomUUID()), selectedOptionKey);
        var change = new MasteryChange(Optional.ofNullable(previous).map(MasteryProbability::new), new MasteryProbability(current));
        return ExerciseAttempt.record(command, answerKey(subtopicId), change);
    }

    public static SubtopicMastery mastery(String holderId, CourseId courseId, SubtopicId subtopicId,
                                          double initial, double current, MasteryLevel level) {
        var mastery = SubtopicMastery.firstEstimate(new UpdateSubtopicMasteryCommand(holderId, courseId, subtopicId,
                new MasteryEstimate(new MasteryProbability(initial), MasteryLevel.ofPercentage(initial * 100))));
        mastery.apply(new UpdateSubtopicMasteryCommand(holderId, courseId, subtopicId,
                new MasteryEstimate(new MasteryProbability(current), level)));
        return mastery;
    }
}
