package com.kalibra.api.progress.domain.services;

import com.kalibra.api.progress.domain.model.commands.RequestPracticeExerciseCommand;
import com.kalibra.api.progress.domain.model.valueobjects.PracticeExerciseView;

import java.util.Optional;

public interface ExerciseAttemptCommandService {

    Optional<PracticeExerciseView> handle(RequestPracticeExerciseCommand command);
}