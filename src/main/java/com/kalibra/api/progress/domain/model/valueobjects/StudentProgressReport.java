package com.kalibra.api.progress.domain.model.valueobjects;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record StudentProgressReport(
        UUID studentId,
        UUID courseId,
        boolean hasActivity,
        Instant lastActivityAt,
        List<SubtopicProgressLine> subtopics,
        List<Feedback> recentFeedback
) {
}