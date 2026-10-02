package com.kalibra.api.progress.domain.model.queries;

import com.kalibra.api.progress.domain.model.valueobjects.CourseId;

public record GetStudentProgressByCourseQuery(
        String holderId,
        CourseId courseId
) {
}
