package com.kalibra.api.progress.application.internal.commandservices;

import com.kalibra.api.progress.application.internal.outboundservices.acl.ExternalCurriculumService;
import com.kalibra.api.progress.application.internal.outboundservices.acl.ExternalEnrollmentService;
import com.kalibra.api.progress.application.internal.outboundservices.acl.ExternalMasteryEstimationService;
import com.kalibra.api.progress.domain.exceptions.ExerciseNotFoundException;
import com.kalibra.api.progress.domain.exceptions.NotEnrolledInCourseException;
import com.kalibra.api.progress.domain.model.aggregates.ExerciseAttempt;
import com.kalibra.api.progress.domain.model.aggregates.SubtopicMastery;
import com.kalibra.api.progress.domain.model.commands.RequestPracticeExerciseCommand;
import com.kalibra.api.progress.domain.model.commands.SubmitAnswerCommand;
import com.kalibra.api.progress.domain.model.events.AnswerRecorded;
import com.kalibra.api.progress.domain.model.valueobjects.AnswerResult;
import com.kalibra.api.progress.domain.model.valueobjects.CourseId;
import com.kalibra.api.progress.domain.model.valueobjects.MasteryChange;
import com.kalibra.api.progress.domain.model.valueobjects.MasteryEstimate;
import com.kalibra.api.progress.domain.model.valueobjects.MasteryProbability;
import com.kalibra.api.progress.domain.model.valueobjects.PracticeExerciseView;
import com.kalibra.api.progress.domain.model.valueobjects.PracticeMode;
import com.kalibra.api.progress.domain.model.valueobjects.StudentId;
import com.kalibra.api.progress.domain.model.valueobjects.SubtopicId;
import com.kalibra.api.progress.domain.repositories.ExerciseAttemptRepository;
import com.kalibra.api.progress.domain.repositories.SubtopicMasteryRepository;
import com.kalibra.api.progress.domain.services.ExerciseAttemptCommandService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class ExerciseAttemptCommandServiceImpl
        implements ExerciseAttemptCommandService {

    private final ExerciseAttemptRepository exerciseAttemptRepository;
    private final SubtopicMasteryRepository subtopicMasteryRepository;
    private final ExternalCurriculumService externalCurriculumService;
    private final ExternalEnrollmentService externalEnrollmentService;
    private final ExternalMasteryEstimationService externalMasteryEstimationService;
    private final ApplicationEventPublisher eventPublisher;
    private final TransactionTemplate transaction;
    private final ConcurrentMap<ExpressSelection, UUID> expressCursors = new ConcurrentHashMap<>();

    private record ExpressSelection(String holderId, CourseId courseId) { }
    private record Candidate(SubtopicId subtopicId, Optional<MasteryProbability> mastery) {
        int priority() {
            return mastery.map(value -> value.value() < 0.4 ? 0 : 2).orElse(1);
        }
    }

    public ExerciseAttemptCommandServiceImpl(
            ExerciseAttemptRepository exerciseAttemptRepository,
            SubtopicMasteryRepository subtopicMasteryRepository,
            ExternalCurriculumService externalCurriculumService,
            ExternalEnrollmentService externalEnrollmentService,
            ExternalMasteryEstimationService externalMasteryEstimationService,
            ApplicationEventPublisher eventPublisher,
            PlatformTransactionManager transactionManager) {
        this.exerciseAttemptRepository = exerciseAttemptRepository;
        this.subtopicMasteryRepository = subtopicMasteryRepository;
        this.externalCurriculumService = externalCurriculumService;
        this.externalEnrollmentService = externalEnrollmentService;
        this.externalMasteryEstimationService = externalMasteryEstimationService;
        this.eventPublisher = eventPublisher;
        // The engine call stays outside the transaction; only saving the attempt and
        // publishing its event run inside one, so the AFTER_COMMIT listener fires.
        this.transaction = new TransactionTemplate(transactionManager);
    }

    @Override
    public ExerciseAttempt handle(SubmitAnswerCommand command) {
        requireEnrollment(command.holderId(), command.courseId());
        var answerKey = externalCurriculumService
                .fetchAnswerKey(command.exerciseId())
                .filter(key -> belongsToCourse(key.subtopicId(), command.courseId()))
                .orElseThrow(ExerciseNotFoundException::new);
        var result = answerKey.isCorrect(command.selectedOptionKey())
                ? AnswerResult.CORRECT
                : AnswerResult.INCORRECT;
        var prior = currentMastery(command.holderId(), answerKey.subtopicId());
        var estimate = externalMasteryEstimationService.estimate(
                command.holderId(), answerKey.subtopicId(), prior, result);
        var change = new MasteryChange(
                prior.or(estimate::prior),
                estimate.probability()
        );
        var attempt = ExerciseAttempt.record(command, answerKey, change);
        return transaction.execute(status -> record(attempt, estimate));
    }

    @Override
    public Optional<PracticeExerciseView> handle(
            RequestPracticeExerciseCommand command) {
        requireEnrollment(command.holderId(), command.courseId());
        if (command.mode() == PracticeMode.EXPRESS) {
            return expressExercise(command);
        }
        if (command.subtopicId() == null) {
            throw new IllegalArgumentException("Selecciona un subtema para la práctica dirigida");
        }
        var mastery = currentMastery(command.holderId(), command.subtopicId());
        return provideExercise(command, mastery);
    }

    private static final int EXPRESS_MAX_TRIES = 2;

    private Optional<PracticeExerciseView> expressExercise(RequestPracticeExerciseCommand command) {
        if (command.subtopicId() != null) {
            throw new IllegalArgumentException("La práctica express selecciona el subtema automáticamente");
        }
        var candidates = externalCurriculumService.fetchPracticableSubtopics(command.courseId()).stream()
                .map(subtopic -> new SubtopicId(subtopic.subtopicId()))
                .map(id -> new Candidate(id, currentMastery(command.holderId(), id)))
                .sorted(Comparator.comparingInt(Candidate::priority)
                        .thenComparingDouble(candidate -> candidate.mastery().map(MasteryProbability::value).orElse(0.0)))
                .toList();
        if (candidates.isEmpty()) {
            return Optional.empty();
        }
        var key = new ExpressSelection(command.holderId(), command.courseId());
        var recent = exerciseAttemptRepository.findFirstByHolderIdAndCourseIdOrderByAnsweredAtDesc(
                command.holderId(), command.courseId()).map(attempt -> attempt.getSubtopicId().value());
        var selectedId = expressCursors.compute(key, (ignored, previous) -> {
            int start = 0;
            for (int index = 0; index < candidates.size(); index++) {
                if (candidates.get(index).subtopicId().value().equals(previous)) {
                    start = (index + 1) % candidates.size();
                    break;
                }
            }
            for (int offset = 0; offset < candidates.size(); offset++) {
                var id = candidates.get((start + offset) % candidates.size()).subtopicId().value();
                if (candidates.size() == 1 || recent.filter(id::equals).isEmpty()) {
                    return id;
                }
            }
            return candidates.get(start).subtopicId().value();
        });
        // A subtopic can yield no verified exercise at its level (every candidate discarded); then the
        // next candidate in rotation is tried so an express session does not stop. Bounded to keep latency.
        int first = 0;
        for (int index = 0; index < candidates.size(); index++) {
            if (candidates.get(index).subtopicId().value().equals(selectedId)) {
                first = index;
                break;
            }
        }
        int tries = Math.min(EXPRESS_MAX_TRIES, candidates.size());
        for (int offset = 0; offset < tries; offset++) {
            var candidate = candidates.get((first + offset) % candidates.size());
            var exercise = provideExercise(new RequestPracticeExerciseCommand(
                    command.holderId(), command.courseId(), candidate.subtopicId()), candidate.mastery());
            if (exercise.isPresent()) {
                expressCursors.put(key, candidate.subtopicId().value());
                return exercise;
            }
        }
        return Optional.empty();
    }

    private Optional<PracticeExerciseView> provideExercise(RequestPracticeExerciseCommand command,
                                                          Optional<MasteryProbability> mastery) {
        var excluded = exerciseAttemptRepository.findAnsweredExerciseIdsByHolderIdAndSubtopicId(
                command.holderId(), command.subtopicId());
        return externalCurriculumService.provideExercise(command, mastery, excluded);
    }

    private ExerciseAttempt record(ExerciseAttempt attempt, MasteryEstimate estimate) {
        var saved = exerciseAttemptRepository.save(attempt);
        eventPublisher.publishEvent(new AnswerRecorded(
                saved.getId().value(),
                saved.getHolderId(),
                saved.getCourseId().value(),
                saved.getSubtopicId().value(),
                estimate.probability().value(),
                estimate.level().name()
        ));
        return saved;
    }

    private void requireEnrollment(String holderId, CourseId courseId) {
        StudentId studentId;
        try {
            studentId = new StudentId(UUID.fromString(holderId));
        } catch (IllegalArgumentException notAStudentId) {
            throw new NotEnrolledInCourseException();
        }
        if (!externalEnrollmentService.isEnrolled(studentId, courseId)) {
            throw new NotEnrolledInCourseException();
        }
    }

    private boolean belongsToCourse(SubtopicId subtopicId, CourseId courseId) {
        return externalCurriculumService.fetchSubtopics(courseId)
                .stream()
                .anyMatch(subtopic -> subtopic.subtopicId().equals(subtopicId.value()));
    }

    // Self-healing: the last recorded answer is the truth. If the handler that updates
    // SubtopicMastery failed after a commit, the next answer still starts from the right value.
    private Optional<MasteryProbability> currentMastery(String holderId, SubtopicId subtopicId) {
        return exerciseAttemptRepository
                .findFirstByHolderIdAndSubtopicIdOrderByAnsweredAtDesc(holderId, subtopicId)
                .map(attempt -> attempt.getMasteryChange().current())
                .or(() -> subtopicMasteryRepository
                        .findByHolderIdAndSubtopicId(holderId, subtopicId)
                        .map(SubtopicMastery::getCurrentEstimate));
    }
}
