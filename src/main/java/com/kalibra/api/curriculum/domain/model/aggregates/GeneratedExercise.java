package com.kalibra.api.curriculum.domain.model.aggregates;

import com.kalibra.api.curriculum.domain.model.commands.GenerateExerciseForStudentCommand;
import com.kalibra.api.curriculum.domain.model.commands.GenerateExercisesForSubtopicCommand;
import com.kalibra.api.curriculum.domain.model.entities.ExerciseOption;
import com.kalibra.api.curriculum.domain.model.valueobjects.CourseId;
import com.kalibra.api.curriculum.domain.model.valueobjects.DifficultyLevel;
import com.kalibra.api.curriculum.domain.model.valueobjects.ExerciseId;
import com.kalibra.api.curriculum.domain.model.valueobjects.ExerciseOrigin;
import com.kalibra.api.curriculum.domain.model.valueobjects.SubtopicId;
import com.kalibra.api.curriculum.domain.model.valueobjects.VerificationOutcome;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class GeneratedExercise {

    public static final int OPTIONS_PER_EXERCISE = 4;

    private ExerciseId id;
    private CourseId courseId;
    private SubtopicId subtopicId;
    private ExerciseOrigin origin;
    private String statement;
    private List<ExerciseOption> options;
    private String explanation;
    private DifficultyLevel difficulty;
    private VerificationOutcome verification;
    private Instant generatedAt;

    // public: required by MapStruct, which generates its mapper impl in a different package.
    public GeneratedExercise() {
    }

    private GeneratedExercise(CourseId courseId, SubtopicId subtopicId, ExerciseOrigin origin, VerificationOutcome verification) {
        if (verification == null) {
            throw new IllegalArgumentException("A generated exercise needs its verification outcome");
        }
        this.id = new ExerciseId(UUID.randomUUID());
        this.courseId = courseId;
        this.subtopicId = subtopicId;
        this.origin = origin;
        this.options = List.of();
        this.verification = verification;
        this.generatedAt = Instant.now();
    }

    public static GeneratedExercise fromEngine(GenerateExerciseForStudentCommand command, VerificationOutcome verification) {
        return new GeneratedExercise(command.courseId(), command.subtopicId(), ExerciseOrigin.STUDENT_PRACTICE, verification);
    }

    public static GeneratedExercise fromEngine(GenerateExercisesForSubtopicCommand command, VerificationOutcome verification) {
        return new GeneratedExercise(command.courseId(), command.subtopicId(), ExerciseOrigin.TEACHER_REQUEST, verification);
    }

    public GeneratedExercise withContent(String statement, List<ExerciseOption> options, String explanation, DifficultyLevel difficulty) {
        if (statement == null || statement.isBlank()) {
            throw new IllegalArgumentException("Exercise statement cannot be blank");
        }
        if (options == null || options.size() != OPTIONS_PER_EXERCISE) {
            throw new IllegalArgumentException("A multiple-choice exercise has exactly " + OPTIONS_PER_EXERCISE + " options");
        }
        if (options.stream().filter(ExerciseOption::isCorrect).count() != 1) {
            throw new IllegalArgumentException("A multiple-choice exercise has exactly one correct option");
        }
        if (options.stream().map(ExerciseOption::getKey).distinct().count() != OPTIONS_PER_EXERCISE) {
            throw new IllegalArgumentException("Exercise option keys must be different");
        }
        if (difficulty == null) {
            throw new IllegalArgumentException("Exercise difficulty cannot be null");
        }
        this.statement = statement;
        this.options = List.copyOf(options);
        this.explanation = explanation == null ? "" : explanation;
        this.difficulty = difficulty;
        return this;
    }

    public boolean isAvailableToStudents() {
        return verification != null && verification.isApproved() && statement != null && !options.isEmpty();
    }

    public ExerciseOption correctOption() {
        return options.stream()
                .filter(ExerciseOption::isCorrect)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("The exercise has no correct option"));
    }

    public ExerciseId getId() {
        return id;
    }

    public CourseId getCourseId() {
        return courseId;
    }

    public SubtopicId getSubtopicId() {
        return subtopicId;
    }

    public ExerciseOrigin getOrigin() {
        return origin;
    }

    public String getStatement() {
        return statement;
    }

    public List<ExerciseOption> getOptions() {
        return options;
    }

    public String getExplanation() {
        return explanation;
    }

    public DifficultyLevel getDifficulty() {
        return difficulty;
    }

    public VerificationOutcome getVerification() {
        return verification;
    }

    public Instant getGeneratedAt() {
        return generatedAt;
    }

    public void setId(ExerciseId id) {
        this.id = id;
    }

    public void setCourseId(CourseId courseId) {
        this.courseId = courseId;
    }

    public void setSubtopicId(SubtopicId subtopicId) {
        this.subtopicId = subtopicId;
    }

    public void setOrigin(ExerciseOrigin origin) {
        this.origin = origin;
    }

    public void setStatement(String statement) {
        this.statement = statement;
    }

    public void setOptions(List<ExerciseOption> options) {
        this.options = options;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public void setDifficulty(DifficultyLevel difficulty) {
        this.difficulty = difficulty;
    }

    public void setVerification(VerificationOutcome verification) {
        this.verification = verification;
    }

    public void setGeneratedAt(Instant generatedAt) {
        this.generatedAt = generatedAt;
    }
}
