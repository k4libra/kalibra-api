package com.kalibra.api.enrollment.domain.model.queries;

import com.kalibra.api.enrollment.domain.model.valueobjects.StudentId;

public record GetEnrolledCoursesByStudentQuery(StudentId studentId) { }
