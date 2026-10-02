package com.kalibra.api.progress.domain.model.queries;

import com.kalibra.api.progress.domain.model.valueobjects.CourseId;

public record ExportCourseIndicatorsQuery(
        String holderId,
        CourseId courseId
) {
}
