package com.kalibra.api.enrollment.domain.services;

import com.kalibra.api.enrollment.domain.model.aggregates.Enrollment;
import com.kalibra.api.enrollment.domain.model.queries.*;
import com.kalibra.api.enrollment.domain.model.valueobjects.CourseRosterGroup;

import java.util.List;
import java.util.Optional;

public interface EnrollmentQueryService {

    List<CourseRosterGroup> handle(
            GetEnrollmentRostersByHolderIdQuery query
    );

    List<Enrollment> handle(
            GetEnrollmentsByCourseQuery query
    );

    Optional<Enrollment> handle(
            GetEnrollmentByStudentAndCourseQuery query
    );
}