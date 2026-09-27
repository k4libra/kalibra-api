package com.kalibra.api.curriculum.domain.services;

import com.kalibra.api.curriculum.domain.model.aggregates.Course;
import com.kalibra.api.curriculum.domain.model.queries.GetCourseByIdQuery;
import com.kalibra.api.curriculum.domain.model.queries.GetCoursesByHolderIdQuery;

import java.util.List;
import java.util.Optional;

public interface CourseQueryService {

    List<Course> handle(GetCoursesByHolderIdQuery query);

    Optional<Course> handle(GetCourseByIdQuery query);
}
