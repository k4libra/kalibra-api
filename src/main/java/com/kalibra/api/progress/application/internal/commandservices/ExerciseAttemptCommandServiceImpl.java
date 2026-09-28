package com.kalibra.api.progress.application.internal.commandservices;

import com.kalibra.api.progress.application.internal.outboundservices.acl.ExternalCurriculumService;
import com.kalibra.api.progress.application.internal.outboundservices.acl.ExternalEnrollmentService;
import com.kalibra.api.progress.domain.exceptions.NotEnrolledInCourseException;
import com.kalibra.api.progress.domain.model.commands.RequestPracticeExerciseCommand;
import com.kalibra.api.progress.domain.model.valueobjects.MasteryProbability;
import com.kalibra.api.progress.domain.model.valueobjects.PracticeExerciseView;
import com.kalibra.api.progress.domain.model.valueobjects.StudentId;
import com.kalibra.api.progress.domain.repositories.SubtopicMasteryRepository;
import com.kalibra.api.progress.domain.services.ExerciseAttemptCommandService;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class ExerciseAttemptCommandServiceImpl
        implements ExerciseAttemptCommandService {

    private final SubtopicMasteryRepository subtopicMasteryRepository;
    private final ExternalCurriculumService externalCurriculumService;
    private final ExternalEnrollmentService externalEnrollmentService;

    public ExerciseAttemptCommandServiceImpl(
            SubtopicMasteryRepository subtopicMasteryRepository,
            ExternalCurriculumService externalCurriculumService,
            ExternalEnrollmentService externalEnrollmentService) {

        this.subtopicMasteryRepository = subtopicMasteryRepository;
        this.externalCurriculumService = externalCurriculumService;
        this.externalEnrollmentService = externalEnrollmentService;
    }

    @Override
    public Optional<PracticeExerciseView> handle(
            RequestPracticeExerciseCommand command) {

        var studentId = new StudentId(
                UUID.fromString(command.holderId())
        );

        if (!externalEnrollmentService.isEnrolled(
                studentId,
                command.courseId())) {
            throw new NotEnrolledInCourseException();
        }

        Optional<MasteryProbability> mastery =
                subtopicMasteryRepository
                        .findByHolderIdAndSubtopicId(
                                command.holderId(),
                                command.subtopicId())
                        .map(subtopicMastery ->
                                subtopicMastery.getCurrentEstimate());

        return externalCurriculumService.provideExercise(
                command,
                mastery
        );
    }
}