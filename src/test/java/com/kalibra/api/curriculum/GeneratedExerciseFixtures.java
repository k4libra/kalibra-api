package com.kalibra.api.curriculum;

import com.kalibra.api.curriculum.domain.model.aggregates.Course;
import com.kalibra.api.curriculum.domain.model.aggregates.GeneratedExercise;
import com.kalibra.api.curriculum.domain.model.commands.GenerateExercisesForSubtopicCommand;
import com.kalibra.api.curriculum.domain.model.entities.ExerciseOption;
import com.kalibra.api.curriculum.domain.model.valueobjects.DifficultyLevel;
import com.kalibra.api.curriculum.domain.model.valueobjects.SubtopicId;
import com.kalibra.api.curriculum.domain.model.valueobjects.VerificationOutcome;
import com.kalibra.api.curriculum.domain.model.valueobjects.VerificationVerdict;

import java.util.List;
import java.util.Optional;

public final class GeneratedExerciseFixtures {

    public static final String REJECTION_REASON = "The statement has no single correct answer";

    private GeneratedExerciseFixtures() {
    }

    public static GeneratedExercise approved(Course course, SubtopicId subtopicId) {
        var outcome = new VerificationOutcome(VerificationVerdict.APPROVED, true, true, Optional.empty(), false);
        return exercise(course, subtopicId, outcome, "Solve 2x + 3 = 7");
    }

    public static GeneratedExercise discarded(Course course, SubtopicId subtopicId) {
        var outcome = new VerificationOutcome(VerificationVerdict.DISCARDED, false, true, Optional.of(REJECTION_REASON), true);
        return exercise(course, subtopicId, outcome, "Solve x = x");
    }

    private static GeneratedExercise exercise(Course course, SubtopicId subtopicId, VerificationOutcome outcome, String statement) {
        var command = new GenerateExercisesForSubtopicCommand(course.getHolderId(), course.getId(), subtopicId, 1);
        var options = List.of(new ExerciseOption("A", "x = 1", false), new ExerciseOption("B", "x = 2", true),
                new ExerciseOption("C", "x = 3", false), new ExerciseOption("D", "x = 5", false));
        return GeneratedExercise.fromEngine(command, outcome)
                .withContent(statement, options, "Subtract 3, divide by 2", DifficultyLevel.EASY);
    }
}
