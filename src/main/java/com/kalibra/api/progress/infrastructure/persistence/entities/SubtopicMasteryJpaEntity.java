package com.kalibra.api.progress.infrastructure.persistence.entities;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "subtopic_masteries", schema = "progress",
        uniqueConstraints = @UniqueConstraint(name = "uq_subtopic_masteries_holder_subtopic", columnNames = {"holder_id", "subtopic_id"}))
public class SubtopicMasteryJpaEntity {

    @Id
    private UUID id;

    @Column(name = "holder_id", nullable = false, length = 64)
    private String holderId;

    @Column(name = "course_id", nullable = false)
    private UUID courseId;

    @Column(name = "subtopic_id", nullable = false)
    private UUID subtopicId;

    @Column(name = "initial_estimate", nullable = false)
    private double initialEstimate;

    @Column(name = "current_estimate", nullable = false)
    private double currentEstimate;

    @Column(nullable = false, length = 10)
    private String level;

    @Column(name = "estimates_count", nullable = false)
    private int estimatesCount;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // public: required by MapStruct, which generates its mapper impl in a different package.
    public SubtopicMasteryJpaEntity() {
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

    public double getInitialEstimate() {
        return initialEstimate;
    }

    public void setInitialEstimate(double initialEstimate) {
        this.initialEstimate = initialEstimate;
    }

    public double getCurrentEstimate() {
        return currentEstimate;
    }

    public void setCurrentEstimate(double currentEstimate) {
        this.currentEstimate = currentEstimate;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public int getEstimatesCount() {
        return estimatesCount;
    }

    public void setEstimatesCount(int estimatesCount) {
        this.estimatesCount = estimatesCount;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}