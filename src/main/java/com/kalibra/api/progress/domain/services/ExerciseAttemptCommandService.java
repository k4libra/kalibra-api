package com.kalibra.api.progress.domain.services;

import com.kalibra.api.progress.domain.model.aggregates.ExerciseAttempt;
import com.kalibra.api.progress.domain.model.commands.RequestPracticeExerciseCommand;
import com.kalibra.api.progress.domain.model.commands.SubmitAnswerCommand;
import com.kalibra.api.progress.domain.model.valueobjects.PracticeExerciseView;

import java.util.Optional;

public interface ExerciseAttemptCommandService {

    ExerciseAttempt handle(SubmitAnswerCommand command);

    Optional<PracticeExerciseView> handle(RequestPracticeExerciseCommand command);
}
