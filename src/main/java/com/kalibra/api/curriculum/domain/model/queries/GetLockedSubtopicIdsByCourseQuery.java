package com.kalibra.api.curriculum.domain.model.queries;

import com.kalibra.api.curriculum.domain.model.valueobjects.CourseId;

public record GetLockedSubtopicIdsByCourseQuery(CourseId courseId, String holderId) { }
