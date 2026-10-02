package com.kalibra.api.progress.domain.model.aggregates;

import com.kalibra.api.progress.domain.model.commands.SubmitAnswerCommand;
import com.kalibra.api.progress.domain.model.valueobjects.AnswerKey;
import com.kalibra.api.progress.domain.model.valueobjects.AnswerResult;
import com.kalibra.api.progress.domain.model.valueobjects.AttemptId;
import com.kalibra.api.progress.domain.model.valueobjects.CourseId;
import com.kalibra.api.progress.domain.model.valueobjects.ExerciseId;
import com.kalibra.api.progress.domain.model.valueobjects.Feedback;
import com.kalibra.api.progress.domain.model.valueobjects.MasteryChange;
import com.kalibra.api.progress.domain.model.valueobjects.SubtopicId;

import java.time.Instant;
import java.util.UUID;

public class ExerciseAttempt {

    private AttemptId id;
    private String holderId;
    private CourseId courseId;
    private SubtopicId subtopicId;
    private ExerciseId exerciseId;
    private String exerciseStatement;
    private String selectedOptionKey;
    private AnswerResult result;
    private Feedback feedback;
    private MasteryChange masteryChange;
    private Instant answeredAt;

    // public: required by MapStruct, which generates its mapper impl in a different package.
    public ExerciseAttempt() {
    }

    public static ExerciseAttempt record(
            SubmitAnswerCommand command,
            AnswerKey answerKey,
            MasteryChange change) {
        if (command.selectedOptionKey() == null || command.selectedOptionKey().isBlank()) {
            throw new IllegalArgumentException("An answer needs the selected option");
        }
        if (change == null) {
            throw new IllegalArgumentException("An answer is recorded with its mastery change");
        }
        var attempt = new ExerciseAttempt();
        attempt.id = new AttemptId(UUID.randomUUID());
        attempt.holderId = command.holderId();
        attempt.courseId = command.courseId();
        attempt.subtopicId = answerKey.subtopicId();
        attempt.exerciseId = command.exerciseId();
        attempt.exerciseStatement = answerKey.statement();
        attempt.selectedOptionKey = command.selectedOptionKey().trim().toUpperCase();
        attempt.result = answerKey.isCorrect(command.selectedOptionKey())
                ? AnswerResult.CORRECT
                : AnswerResult.INCORRECT;
        attempt.feedback = new Feedback(answerKey.explanation());
        attempt.masteryChange = change;
        attempt.answeredAt = Instant.now();
        return attempt;
    }

    public boolean isCorrect() {
        return result == AnswerResult.CORRECT;
    }

    public AttemptId getId() {
        return id;
    }

    public String getHolderId() {
        return holderId;
    }

    public CourseId getCourseId() {
        return courseId;
    }

    public SubtopicId getSubtopicId() {
        return subtopicId;
    }

    public ExerciseId getExerciseId() {
        return exerciseId;
    }

    public String getExerciseStatement() {
        return exerciseStatement;
    }

    public String getSelectedOptionKey() {
        return selectedOptionKey;
    }

    public AnswerResult getResult() {
        return result;
    }

    public Feedback getFeedback() {
        return feedback;
    }

    public MasteryChange getMasteryChange() {
        return masteryChange;
    }

    public Instant getAnsweredAt() {
        return answeredAt;
    }

    public void setId(AttemptId id) {
        this.id = id;
    }

    public void setHolderId(String holderId) {
        this.holderId = holderId;
    }

    public void setCourseId(CourseId courseId) {
        this.courseId = courseId;
    }

    public void setSubtopicId(SubtopicId subtopicId) {
        this.subtopicId = subtopicId;
    }

    public void setExerciseId(ExerciseId exerciseId) {
        this.exerciseId = exerciseId;
    }

    public void setExerciseStatement(String exerciseStatement) {
        this.exerciseStatement = exerciseStatement;
    }

    public void setSelectedOptionKey(String selectedOptionKey) {
        this.selectedOptionKey = selectedOptionKey;
    }

    public void setResult(AnswerResult result) {
        this.result = result;
    }

    public void setFeedback(Feedback feedback) {
        this.feedback = feedback;
    }

    public void setMasteryChange(MasteryChange masteryChange) {
        this.masteryChange = masteryChange;
    }

    public void setAnsweredAt(Instant answeredAt) {
        this.answeredAt = answeredAt;
    }
}
