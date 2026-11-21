package com.kalibra.api.curriculum.domain.model.commands;

import com.kalibra.api.curriculum.domain.model.valueobjects.CourseId;
import com.kalibra.api.curriculum.domain.model.valueobjects.DifficultyLevel;
import com.kalibra.api.curriculum.domain.model.valueobjects.ExerciseId;
import com.kalibra.api.curriculum.domain.model.valueobjects.MasteryProbability;
import com.kalibra.api.curriculum.domain.model.valueobjects.SubtopicId;

import java.util.Optional;
import java.util.Set;

public record GenerateExerciseForStudentCommand(CourseId courseId, SubtopicId subtopicId,
                                               Optional<MasteryProbability> mastery,
                                               Set<ExerciseId> excludedExerciseIds,
                                               Optional<DifficultyLevel> targetDifficulty) {
    public GenerateExerciseForStudentCommand {
        excludedExerciseIds = Set.copyOf(excludedExerciseIds);
    }
}
