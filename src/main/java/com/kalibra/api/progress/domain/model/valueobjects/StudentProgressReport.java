package com.kalibra.api.progress.domain.model.valueobjects;

import java.util.List;
import java.util.UUID;

public record StudentProgressReport(
        UUID studentId,
        UUID courseId,
        boolean hasActivity,
        List<SubtopicProgressLine> subtopics,
        List<Feedback> recentFeedback
) {
}