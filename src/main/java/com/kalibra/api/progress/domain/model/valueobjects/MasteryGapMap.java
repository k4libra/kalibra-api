package com.kalibra.api.progress.domain.model.valueobjects;

import java.util.List;
import java.util.UUID;

public record MasteryGapMap(
        UUID courseId,
        boolean hasSufficientData,
        List<SubtopicGapLine> subtopics,
        List<StudentMasteryCell> students
) {
}