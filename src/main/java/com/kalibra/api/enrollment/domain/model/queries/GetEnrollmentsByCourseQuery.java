package com.kalibra.api.enrollment.domain.model.queries;

import com.kalibra.api.enrollment.domain.model.valueobjects.CourseId;

public record GetEnrollmentsByCourseQuery(
        CourseId courseId
) {
}