package com.kalibra.api.enrollment.domain.model.queries;

import com.kalibra.api.enrollment.domain.model.valueobjects.CourseId;
import com.kalibra.api.enrollment.domain.model.valueobjects.StudentId;

public record GetEnrollmentByStudentAndCourseQuery(
        StudentId studentId,
        CourseId courseId
) {
}