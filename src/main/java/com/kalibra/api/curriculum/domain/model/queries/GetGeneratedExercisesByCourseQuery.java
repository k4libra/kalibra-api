package com.kalibra.api.curriculum.domain.model.queries;

import com.kalibra.api.curriculum.domain.model.valueobjects.CourseId;
import com.kalibra.api.curriculum.domain.model.valueobjects.Pagination;
import com.kalibra.api.curriculum.domain.model.valueobjects.SubtopicId;

import java.util.Optional;

public record GetGeneratedExercisesByCourseQuery(String holderId, CourseId courseId, Optional<SubtopicId> subtopicId, Pagination pagination) { }
