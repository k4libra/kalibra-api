package com.kalibra.api.progress.domain.model.aggregates;

import com.kalibra.api.progress.ProgressFixtures;
import com.kalibra.api.progress.domain.model.commands.SubmitAnswerCommand;
import com.kalibra.api.progress.domain.model.valueobjects.AnswerResult;
import com.kalibra.api.progress.domain.model.valueobjects.CourseId;
import com.kalibra.api.progress.domain.model.valueobjects.ExerciseId;
import com.kalibra.api.progress.domain.model.valueobjects.MasteryChange;
import com.kalibra.api.progress.domain.model.valueobjects.MasteryProbability;
import com.kalibra.api.progress.domain.model.valueobjects.SubtopicId;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExerciseAttemptTest {

    private final CourseId courseId = new CourseId(UUID.randomUUID());
    private final SubtopicId subtopicId = new SubtopicId(UUID.randomUUID());
    private final ExerciseId exerciseId = new ExerciseId(UUID.randomUUID());
    private final MasteryChange change =
            new MasteryChange(Optional.of(new MasteryProbability(0.30)), new MasteryProbability(0.52));

    @Test
    void shouldRecordACorrectAnswerWithItsFeedbackAndMasteryChange() {
        var command = new SubmitAnswerCommand("student-1", courseId, exerciseId, " b ");

        var attempt = ExerciseAttempt.record(command, ProgressFixtures.answerKey(subtopicId), change);

        assertThat(attempt.isCorrect()).isTrue();
        assertThat(attempt.getResult()).isEqualTo(AnswerResult.CORRECT);
        assertThat(attempt.getSelectedOptionKey()).isEqualTo("B");
        assertThat(attempt.getSubtopicId()).isEqualTo(subtopicId);
        assertThat(attempt.getExerciseStatement()).isEqualTo("Solve 2x + 3 = 7");
        assertThat(attempt.getFeedback().explanation()).isEqualTo(ProgressFixtures.EXPLANATION);
        assertThat(attempt.getMasteryChange().deltaPoints()).isEqualTo(22);
        assertThat(attempt.getHolderId()).isEqualTo("student-1");
        assertThat(attempt.getAnsweredAt()).isNotNull();
    }

    @Test
    void shouldRecordAnIncorrectAnswer() {
        var command = new SubmitAnswerCommand("student-1", courseId, exerciseId, "A");

        var attempt = ExerciseAttempt.record(command, ProgressFixtures.answerKey(subtopicId), change);

        assertThat(attempt.isCorrect()).isFalse();
        assertThat(attempt.getResult()).isEqualTo(AnswerResult.INCORRECT);
    }

    @Test
    void shouldRejectAnAnswerWithoutOptionOrMasteryChange() {
        var withoutOption = new SubmitAnswerCommand("student-1", courseId, exerciseId, " ");
        var command = new SubmitAnswerCommand("student-1", courseId, exerciseId, "A");

        assertThatThrownBy(() -> ExerciseAttempt.record(withoutOption, ProgressFixtures.answerKey(subtopicId), change))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ExerciseAttempt.record(command, ProgressFixtures.answerKey(subtopicId), null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
