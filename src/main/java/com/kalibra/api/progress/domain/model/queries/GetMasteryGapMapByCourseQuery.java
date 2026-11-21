package com.kalibra.api.progress.domain.model.queries;

import com.kalibra.api.progress.domain.model.valueobjects.CourseId;

public record GetMasteryGapMapByCourseQuery(
        String holderId,
        CourseId courseId
) {
}
