package com.kalibra.api.curriculum.domain.services;

import com.kalibra.api.curriculum.domain.model.aggregates.GeneratedExercise;
import com.kalibra.api.curriculum.domain.model.commands.GenerateExerciseForStudentCommand;
import com.kalibra.api.curriculum.domain.model.commands.GenerateExercisesForSubtopicCommand;

import java.util.List;
import java.util.Optional;

public interface GeneratedExerciseCommandService {

    List<GeneratedExercise> handle(GenerateExercisesForSubtopicCommand command);

    Optional<GeneratedExercise> handle(GenerateExerciseForStudentCommand command);
}
