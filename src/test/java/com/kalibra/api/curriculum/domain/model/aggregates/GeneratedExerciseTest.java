package com.kalibra.api.curriculum.domain.model.aggregates;

import com.kalibra.api.curriculum.domain.model.commands.GenerateExerciseForStudentCommand;
import com.kalibra.api.curriculum.domain.model.commands.GenerateExercisesForSubtopicCommand;
import com.kalibra.api.curriculum.domain.model.entities.ExerciseOption;
import com.kalibra.api.curriculum.domain.model.valueobjects.CourseId;
import com.kalibra.api.curriculum.domain.model.valueobjects.DifficultyLevel;
import com.kalibra.api.curriculum.domain.model.valueobjects.ExerciseOrigin;
import com.kalibra.api.curriculum.domain.model.valueobjects.MasteryProbability;
import com.kalibra.api.curriculum.domain.model.valueobjects.SubtopicExerciseCount;
import com.kalibra.api.curriculum.domain.model.valueobjects.SubtopicId;
import com.kalibra.api.curriculum.domain.model.valueobjects.VerificationOutcome;
import com.kalibra.api.curriculum.domain.model.valueobjects.VerificationVerdict;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GeneratedExerciseTest {

    private static final VerificationOutcome APPROVED =
            new VerificationOutcome(VerificationVerdict.APPROVED, true, true, Optional.empty(), false);
    private static final VerificationOutcome DISCARDED =
            new VerificationOutcome(VerificationVerdict.DISCARDED, false, true, Optional.of("Two options are correct"), true);

    private final GenerateExercisesForSubtopicCommand teacherCommand = new GenerateExercisesForSubtopicCommand(
            "teacher-1", new CourseId(UUID.randomUUID()), new SubtopicId(UUID.randomUUID()), 2);
    private final GenerateExerciseForStudentCommand studentCommand = new GenerateExerciseForStudentCommand(
            new CourseId(UUID.randomUUID()), new SubtopicId(UUID.randomUUID()), Optional.of(new MasteryProbability(0.4)));

    private static List<ExerciseOption> options() {
        return List.of(new ExerciseOption("A", "x = 1", false), new ExerciseOption("b", "x = 2", true),
                new ExerciseOption("C", "x = 3", false), new ExerciseOption("D", "x = 5", false));
    }

    @Test
    void shouldKeepTheApprovedExerciseOfATeacherRequestAvailableToStudents() {
        var exercise = GeneratedExercise.fromEngine(teacherCommand, APPROVED)
                .withContent("Solve 2x + 3 = 7", options(), "Subtract 3, divide by 2", DifficultyLevel.EASY);

        assertThat(exercise.getOrigin()).isEqualTo(ExerciseOrigin.TEACHER_REQUEST);
        assertThat(exercise.getCourseId()).isEqualTo(teacherCommand.courseId());
        assertThat(exercise.getSubtopicId()).isEqualTo(teacherCommand.subtopicId());
        assertThat(exercise.isAvailableToStudents()).isTrue();
        assertThat(exercise.correctOption().getKey()).isEqualTo("B");
        assertThat(exercise.getGeneratedAt()).isNotNull();
    }

    @Test
    void shouldNeverMakeADiscardedExerciseAvailableToStudents() {
        var exercise = GeneratedExercise.fromEngine(studentCommand, DISCARDED)
                .withContent("Solve x = x", options(), "Ambiguous", DifficultyLevel.HARD);

        assertThat(exercise.getOrigin()).isEqualTo(ExerciseOrigin.STUDENT_PRACTICE);
        assertThat(exercise.isAvailableToStudents()).isFalse();
        assertThat(exercise.getVerification().rejectionReason()).contains("Two options are correct");
        assertThat(exercise.getVerification().usedFallback()).isTrue();
    }

    @Test
    void shouldNotBeAvailableBeforeItHasContent() {
        assertThat(GeneratedExercise.fromEngine(teacherCommand, APPROVED).isAvailableToStudents()).isFalse();
    }

    @Test
    void shouldRejectContentThatIsNotAFourOptionExerciseWithOneAnswer() {
        var exercise = GeneratedExercise.fromEngine(teacherCommand, APPROVED);
        var threeOptions = options().subList(0, 3);
        var twoCorrect = List.of(new ExerciseOption("A", "1", true), new ExerciseOption("B", "2", true),
                new ExerciseOption("C", "3", false), new ExerciseOption("D", "4", false));
        var repeatedKey = List.of(new ExerciseOption("A", "1", true), new ExerciseOption("A", "2", false),
                new ExerciseOption("C", "3", false), new ExerciseOption("D", "4", false));

        assertThatThrownBy(() -> exercise.withContent("Statement", threeOptions, "", DifficultyLevel.EASY))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> exercise.withContent("Statement", twoCorrect, "", DifficultyLevel.EASY))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> exercise.withContent("Statement", repeatedKey, "", DifficultyLevel.EASY))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> exercise.withContent(" ", options(), "", DifficultyLevel.EASY))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> exercise.withContent("Statement", options(), "", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectAnApprovedOutcomeThatFailedACheck() {
        assertThatThrownBy(() -> new VerificationOutcome(VerificationVerdict.APPROVED, true, false, Optional.empty(), false))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> GeneratedExercise.fromEngine(teacherCommand, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldComputeTheApprovalRateOfASubtopic() {
        assertThat(new SubtopicExerciseCount(UUID.randomUUID(), "Equations", 4, 3, 1).approvalRate()).isEqualTo(75.0);
        assertThat(new SubtopicExerciseCount(UUID.randomUUID(), "Equations", 0, 0, 0).approvalRate()).isZero();
    }

    @Test
    void shouldRejectAMasteryProbabilityOutsideTheUnitInterval() {
        assertThatThrownBy(() -> new MasteryProbability(1.2)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new MasteryProbability(-0.1)).isInstanceOf(IllegalArgumentException.class);
    }
}
