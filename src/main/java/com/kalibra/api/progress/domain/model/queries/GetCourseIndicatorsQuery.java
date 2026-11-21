package com.kalibra.api.progress.domain.model.queries;

import com.kalibra.api.progress.domain.model.valueobjects.CourseId;

public record GetCourseIndicatorsQuery(
        String holderId,
        CourseId courseId
) {
}
