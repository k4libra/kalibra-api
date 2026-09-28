package com.kalibra.api.progress.application.internal.outboundservices.acl;

import com.kalibra.api.progress.domain.model.commands.RequestPracticeExerciseCommand;
import com.kalibra.api.progress.domain.model.valueobjects.AnswerKey;
import com.kalibra.api.progress.domain.model.valueobjects.ExerciseId;
import com.kalibra.api.progress.domain.model.valueobjects.MasteryProbability;
import com.kalibra.api.progress.domain.model.valueobjects.PracticeExerciseView;

import java.util.Optional;

public class ExternalCurriculumService {

    public Optional<AnswerKey> fetchAnswerKey(ExerciseId exerciseId) {
        return Optional.empty();
    }

    public Optional<PracticeExerciseView> provideExercise(
            RequestPracticeExerciseCommand command,
            Optional<MasteryProbability> mastery) {
        return Optional.empty();
    }
}