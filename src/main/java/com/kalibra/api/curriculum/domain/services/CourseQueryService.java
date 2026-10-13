package com.kalibra.api.curriculum.domain.services;

import com.kalibra.api.curriculum.domain.model.aggregates.Course;
import com.kalibra.api.curriculum.domain.model.queries.GetAllCoursesQuery;
import com.kalibra.api.curriculum.domain.model.queries.GetCourseByIdQuery;
import com.kalibra.api.curriculum.domain.model.queries.GetCoursesByHolderIdQuery;
import com.kalibra.api.curriculum.domain.model.queries.GetLockedSubtopicIdsByCourseQuery;
import com.kalibra.api.curriculum.domain.model.valueobjects.SubtopicId;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface CourseQueryService {

    List<Course> handle(GetCoursesByHolderIdQuery query);

    Optional<Course> handle(GetCourseByIdQuery query);

    List<Course> handle(GetAllCoursesQuery query);

    Set<SubtopicId> handle(GetLockedSubtopicIdsByCourseQuery query);
}
