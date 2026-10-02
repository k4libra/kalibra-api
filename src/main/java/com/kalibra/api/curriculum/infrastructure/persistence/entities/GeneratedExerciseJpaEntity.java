package com.kalibra.api.curriculum.infrastructure.persistence.entities;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import org.hibernate.annotations.BatchSize;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "generated_exercises", schema = "curriculum")
public class GeneratedExerciseJpaEntity {

    @Id
    private UUID id;

    @Column(name = "course_id", nullable = false)
    private UUID courseId;

    @Column(name = "subtopic_id", nullable = false)
    private UUID subtopicId;

    @Column(nullable = false, length = 20)
    private String origin;

    @Column(nullable = false, columnDefinition = "text")
    private String statement;

    // batch: a page of exercises loads its options in one query instead of one per exercise.
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "generated_exercise_options", schema = "curriculum", joinColumns = @JoinColumn(name = "exercise_id"))
    @OrderBy("optionKey ASC")
    @BatchSize(size = 100)
    private List<ExerciseOptionEmbeddable> options = new ArrayList<>();

    @Column(nullable = false, columnDefinition = "text")
    private String explanation;

    @Column(nullable = false, length = 10)
    private String difficulty;

    @Embedded
    private VerificationOutcomeEmbeddable verification;

    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt;

    // public: required by MapStruct, which generates its mapper impl in a different package.
    public GeneratedExerciseJpaEntity() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getStatement() {
        return statement;
    }

    public void setStatement(String statement) {
        this.statement = statement;
    }

    public List<ExerciseOptionEmbeddable> getOptions() {
        return options;
    }

    public void setOptions(List<ExerciseOptionEmbeddable> options) {
        this.options = options;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public VerificationOutcomeEmbeddable getVerification() {
        return verification;
    }

    public void setVerification(VerificationOutcomeEmbeddable verification) {
        this.verification = verification;
    }

    public Instant getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(Instant generatedAt) {
        this.generatedAt = generatedAt;
    }
}
