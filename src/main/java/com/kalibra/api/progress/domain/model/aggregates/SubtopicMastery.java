package com.kalibra.api.progress.domain.model.aggregates;

import com.kalibra.api.progress.domain.model.commands.UpdateSubtopicMasteryCommand;
import com.kalibra.api.progress.domain.model.valueobjects.*;

import java.time.Instant;
import java.util.UUID;

public class SubtopicMastery {

    private SubtopicMasteryId id;
    private String holderId;
    private CourseId courseId;
    private SubtopicId subtopicId;
    private MasteryProbability initialEstimate;
    private MasteryProbability currentEstimate;
    private MasteryLevel level;
    private int estimatesCount;
    private Instant updatedAt;

    // public: required by MapStruct, which generates its mapper impl in a different package.
    public SubtopicMastery() {
    }

    public SubtopicMasteryId getId() {
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

    public MasteryProbability getInitialEstimate() {
        return initialEstimate;
    }

    public MasteryProbability getCurrentEstimate() {
        return currentEstimate;
    }

    public MasteryLevel getLevel() {
        return level;
    }

    public int getEstimatesCount() {
        return estimatesCount;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public static SubtopicMastery firstEstimate(
            UpdateSubtopicMasteryCommand command) {

        var mastery = new SubtopicMastery();

        mastery.id = new SubtopicMasteryId(UUID.randomUUID());
        mastery.holderId = command.holderId();
        mastery.courseId = command.courseId();
        mastery.subtopicId = command.subtopicId();
        mastery.initialEstimate = command.estimate().probability();
        mastery.currentEstimate = command.estimate().probability();
        mastery.level = command.estimate().level();
        mastery.estimatesCount = 1;
        mastery.updatedAt = Instant.now();

        return mastery;
    }

    public void apply(UpdateSubtopicMasteryCommand command) {
        this.currentEstimate = command.estimate().probability();
        this.level = command.estimate().level();
        this.estimatesCount++;
        this.updatedAt = Instant.now();
    }

    public int evolutionPoints() {
        return currentEstimate.asPercentage()
                - initialEstimate.asPercentage();
    }

    public void setId(SubtopicMasteryId id) {
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

    public void setInitialEstimate(MasteryProbability initialEstimate) {
        this.initialEstimate = initialEstimate;
    }

    public void setCurrentEstimate(MasteryProbability currentEstimate) {
        this.currentEstimate = currentEstimate;
    }

    public void setLevel(MasteryLevel level) {
        this.level = level;
    }

    public void setEstimatesCount(int estimatesCount) {
        this.estimatesCount = estimatesCount;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
