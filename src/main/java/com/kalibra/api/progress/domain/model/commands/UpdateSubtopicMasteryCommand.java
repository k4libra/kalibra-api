package com.kalibra.api.progress.domain.model.commands;

import com.kalibra.api.progress.domain.model.valueobjects.CourseId;
import com.kalibra.api.progress.domain.model.valueobjects.MasteryEstimate;
import com.kalibra.api.progress.domain.model.valueobjects.SubtopicId;

public record UpdateSubtopicMasteryCommand(
        String holderId,
        CourseId courseId,
        SubtopicId subtopicId,
        MasteryEstimate estimate
) {
}