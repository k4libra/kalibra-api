package com.kalibra.api.progress.domain.model.aggregates;

import com.kalibra.api.progress.domain.model.valueobjects.CourseId;
import com.kalibra.api.progress.domain.model.valueobjects.MasteryLevel;
import com.kalibra.api.progress.domain.model.valueobjects.MasteryProbability;
import com.kalibra.api.progress.domain.model.valueobjects.SubtopicId;
import com.kalibra.api.progress.domain.model.valueobjects.SubtopicMasteryId;

import java.time.Instant;

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

    public int evolutionPoints() {
        return currentEstimate.asPercentage()
                - initialEstimate.asPercentage();
    }
}