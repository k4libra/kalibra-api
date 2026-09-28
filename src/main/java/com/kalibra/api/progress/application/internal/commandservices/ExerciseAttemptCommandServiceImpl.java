package com.kalibra.api.progress.application.internal.commandservices;

import com.kalibra.api.progress.application.internal.outboundservices.acl.ExternalCurriculumService;
import com.kalibra.api.progress.domain.model.commands.RequestPracticeExerciseCommand;
import com.kalibra.api.progress.domain.model.valueobjects.MasteryProbability;
import com.kalibra.api.progress.domain.model.valueobjects.PracticeExerciseView;
import com.kalibra.api.progress.domain.repositories.SubtopicMasteryRepository;
import com.kalibra.api.progress.domain.services.ExerciseAttemptCommandService;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ExerciseAttemptCommandServiceImpl
        implements ExerciseAttemptCommandService {

    private final SubtopicMasteryRepository subtopicMasteryRepository;
    private final ExternalCurriculumService externalCurriculumService;

    public ExerciseAttemptCommandServiceImpl(
            SubtopicMasteryRepository subtopicMasteryRepository,
            ExternalCurriculumService externalCurriculumService) {
        this.subtopicMasteryRepository = subtopicMasteryRepository;
        this.externalCurriculumService = externalCurriculumService;
    }

    @Override
    public Optional<PracticeExerciseView> handle(
            RequestPracticeExerciseCommand command) {

        Optional<MasteryProbability> mastery =
                subtopicMasteryRepository
                        .findByHolderIdAndSubtopicId(
                                command.holderId(),
                                command.subtopicId())
                        .map(subtopicMastery ->
                                subtopicMastery.getCurrentEstimate());

        return externalCurriculumService.provideExercise(command, mastery);
    }
}