package com.kalibra.api.progress.infrastructure.persistence.entities;

import com.kalibra.api.shared.contracts.curriculum.ExplanationStep;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "exercise_attempts", schema = "progress")
public class ExerciseAttemptJpaEntity {

    @Id
    private UUID id;

    @Column(name = "holder_id", nullable = false, length = 64)
    private String holderId;

    @Column(name = "course_id", nullable = false)
    private UUID courseId;

    @Column(name = "subtopic_id", nullable = false)
    private UUID subtopicId;

    @Column(name = "exercise_id", nullable = false)
    private UUID exerciseId;

    @Column(name = "exercise_statement", nullable = false, columnDefinition = "text")
    private String exerciseStatement;

    @Column(name = "selected_option_key", nullable = false, length = 1)
    private String selectedOptionKey;

    @Column(nullable = false, length = 10)
    private String result;

    @Column(name = "feedback_explanation", columnDefinition = "text")
    private String feedbackExplanation;

    @Embedded
    private MasteryChangeEmbeddable masteryChange;

    @Column(name = "answered_at", nullable = false)
    private Instant answeredAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "explanation_steps", columnDefinition = "jsonb")
    private List<ExplanationStep> explanationSteps;

    @Column(name = "key_idea", columnDefinition = "text")
    private String keyIdea;

    @Column(name = "time_spent_seconds")
    private Integer timeSpentSeconds;

    @Column(name = "correct_option_key", length = 1)
    private String correctOptionKey;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> options;

    // public: required by MapStruct, which generates its mapper impl in a different package.
    public ExerciseAttemptJpaEntity() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getHolderId() {
        return holderId;
    }

    public void setHolderId(String holderId) {
        this.holderId = holderId;
    }

    public UUID getCourseId() {
        return courseId;
    }

    public void setCourseId(UUID courseId) {
        this.courseId = courseId;
    }

    public UUID getSubtopicId() {
        return subtopicId;
    }

    public void setSubtopicId(UUID subtopicId) {
        this.subtopicId = subtopicId;
    }

    public UUID getExerciseId() {
        return exerciseId;
    }

    public void setExerciseId(UUID exerciseId) {
        this.exerciseId = exerciseId;
    }

    public String getExerciseStatement() {
        return exerciseStatement;
    }

    public void setExerciseStatement(String exerciseStatement) {
        this.exerciseStatement = exerciseStatement;
    }

    public String getSelectedOptionKey() {
        return selectedOptionKey;
    }

    public void setSelectedOptionKey(String selectedOptionKey) {
        this.selectedOptionKey = selectedOptionKey;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getFeedbackExplanation() {
        return feedbackExplanation;
    }

    public void setFeedbackExplanation(String feedbackExplanation) {
        this.feedbackExplanation = feedbackExplanation;
    }

    public MasteryChangeEmbeddable getMasteryChange() {
        return masteryChange;
    }

    public void setMasteryChange(MasteryChangeEmbeddable masteryChange) {
        this.masteryChange = masteryChange;
    }

    public Instant getAnsweredAt() {
        return answeredAt;
    }

    public void setAnsweredAt(Instant answeredAt) {
        this.answeredAt = answeredAt;
    }

    public List<ExplanationStep> getExplanationSteps() {
        return explanationSteps;
    }

    public void setExplanationSteps(List<ExplanationStep> explanationSteps) {
        this.explanationSteps = explanationSteps;
    }

    public String getKeyIdea() {
        return keyIdea;
    }

    public void setKeyIdea(String keyIdea) {
        this.keyIdea = keyIdea;
    }

    public Integer getTimeSpentSeconds() {
        return timeSpentSeconds;
    }

    public void setTimeSpentSeconds(Integer timeSpentSeconds) {
        this.timeSpentSeconds = timeSpentSeconds;
    }

    public String getCorrectOptionKey() {
        return correctOptionKey;
    }

    public void setCorrectOptionKey(String correctOptionKey) {
        this.correctOptionKey = correctOptionKey;
    }

    public List<String> getOptions() {
        return options;
    }

    public void setOptions(List<String> options) {
        this.options = options;
    }
}
