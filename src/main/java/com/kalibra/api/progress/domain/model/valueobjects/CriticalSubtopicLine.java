package com.kalibra.api.progress.domain.model.valueobjects;

import java.util.UUID;

public record CriticalSubtopicLine(
        UUID courseId,
        String courseName,
        UUID subtopicId,
        String subtopicName,
        double groupMastery,
        int lowCount,
        int mediumCount,
        int highCount,
        int noDataCount
) {
}
