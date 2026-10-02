package com.kalibra.api.progress.infrastructure.persistence.entities;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "subtopic_masteries", schema = "progress")
public class SubtopicMasteryJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String holderId;

    @Column(nullable = false)
    private UUID courseId;

    @Column(nullable = false)
    private UUID subtopicId;

    @Column(nullable = false)
    private double initialEstimate;

    @Column(nullable = false)
    private double currentEstimate;

    @Column(nullable = false)
    private String level;

    @Column(nullable = false)
    private int estimatesCount;

    @Column(nullable = false)
    private Instant updatedAt;

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