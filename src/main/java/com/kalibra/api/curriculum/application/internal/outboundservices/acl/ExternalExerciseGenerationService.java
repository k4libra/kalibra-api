package com.kalibra.api.curriculum.application.internal.outboundservices.acl;

import com.fasterxml.jackson.databind.JsonNode;
import com.kalibra.api.curriculum.domain.model.aggregates.GeneratedExercise;
import com.kalibra.api.curriculum.domain.model.commands.GenerateExerciseForStudentCommand;
import com.kalibra.api.curriculum.domain.model.commands.GenerateExercisesForSubtopicCommand;
import com.kalibra.api.curriculum.domain.model.entities.ExerciseOption;
import com.kalibra.api.curriculum.domain.model.entities.Subtopic;
import com.kalibra.api.curriculum.domain.model.valueobjects.CourseId;
import com.kalibra.api.curriculum.domain.model.valueobjects.CurricularContent;
import com.kalibra.api.curriculum.domain.model.valueobjects.DifficultyLevel;
import com.kalibra.api.curriculum.domain.model.valueobjects.MasteryProbability;
import com.kalibra.api.curriculum.domain.model.valueobjects.SubtopicId;
import com.kalibra.api.curriculum.domain.model.valueobjects.VerificationOutcome;
import com.kalibra.api.curriculum.domain.model.valueobjects.VerificationVerdict;
import com.kalibra.api.curriculum.domain.repositories.CourseRepository;
import com.kalibra.api.shared.engine.EngineTaskClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

// Returns every attempt of the engine, approved or discarded: the caller persists them all and
// only hands an approved one to a student.
@Service
public class ExternalExerciseGenerationService {

    static final String TASK_TYPE = "exercise-generations";
    private static final String OPTION_KEYS = "ABCD";

    private static final Logger log = LoggerFactory.getLogger(ExternalExerciseGenerationService.class);

    private final EngineTaskClient engineTaskClient;
    private final CourseRepository courseRepository;

    public ExternalExerciseGenerationService(EngineTaskClient engineTaskClient, CourseRepository courseRepository) {
        this.engineTaskClient = engineTaskClient;
        this.courseRepository = courseRepository;
    }

    public List<GeneratedExercise> generate(GenerateExerciseForStudentCommand command, CurricularContent context) {
        var runs = request(command.courseId(), command.subtopicId(), context, command.mastery(), 1);
        return attemptsOf(runs, verification -> GeneratedExercise.fromEngine(command, verification));
    }

    public List<GeneratedExercise> generate(GenerateExercisesForSubtopicCommand command, CurricularContent context) {
        var runs = request(command.courseId(), command.subtopicId(), context, Optional.empty(), command.quantity());
        return attemptsOf(runs, verification -> GeneratedExercise.fromEngine(command, verification));
    }

    private JsonNode request(CourseId courseId, SubtopicId subtopicId, CurricularContent context,
                             Optional<MasteryProbability> mastery, int quantity) {
        var payload = new HashMap<String, Object>();
        payload.put("courseId", courseId.value().toString());
        payload.put("subtopicId", subtopicId.value().toString());
        payload.put("subtopicName", subtopicName(courseId, subtopicId));
        payload.put("normalizedContent", context.normalizedText());
        payload.put("masteryProbability", mastery.map(MasteryProbability::value).orElse(null));
        payload.put("quantity", quantity);
        return engineTaskClient.submit(TASK_TYPE, payload);
    }

    private String subtopicName(CourseId courseId, SubtopicId subtopicId) {
        return courseRepository.findById(courseId).stream()
                .flatMap(course -> course.getSubtopics().stream())
                .filter(subtopic -> subtopic.getId().equals(subtopicId))
                .map(Subtopic::getName)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Subtopic does not belong to the course: " + subtopicId.value()));
    }

    private List<GeneratedExercise> attemptsOf(JsonNode runs, Function<VerificationOutcome, GeneratedExercise> exerciseOf) {
        var exercises = new ArrayList<GeneratedExercise>();
        for (var run : runs) {
            for (var attempt : run.path("attempts")) {
                try {
                    exercises.add(withContent(exerciseOf.apply(verificationOf(attempt)), attempt.path("exercise")));
                } catch (IllegalArgumentException malformed) {
                    // An attempt the domain cannot hold is never stored as a usable exercise.
                    log.warn("Engine attempt {} skipped: {}", attempt.path("number").asInt(), malformed.getMessage());
                }
            }
        }
        return exercises;
    }

    private VerificationOutcome verificationOf(JsonNode attempt) {
        var reason = attempt.path("rejectionReason");
        return new VerificationOutcome(
                attempt.path("approved").asBoolean() ? VerificationVerdict.APPROVED : VerificationVerdict.DISCARDED,
                attempt.path("correctnessPassed").asBoolean(),
                attempt.path("difficultyPassed").asBoolean(),
                reason.isTextual() ? Optional.of(reason.asText()) : Optional.empty(),
                attempt.path("usedFallback").asBoolean());
    }

    // The engine sends the option texts in order A-D and the key of the correct one.
    private GeneratedExercise withContent(GeneratedExercise exercise, JsonNode content) {
        var correctKey = content.path("correctOptionKey").asText("").trim().toUpperCase();
        var options = new ArrayList<ExerciseOption>();
        var position = 0;
        for (var text : content.path("options")) {
            var key = position < OPTION_KEYS.length() ? String.valueOf(OPTION_KEYS.charAt(position)) : String.valueOf(position + 1);
            options.add(new ExerciseOption(key, text.asText(), key.equals(correctKey)));
            position++;
        }
        return exercise.withContent(
                content.path("statement").asText(),
                options,
                content.path("explanation").asText(""),
                DifficultyLevel.valueOf(content.path("difficulty").asText("")));
    }
}
