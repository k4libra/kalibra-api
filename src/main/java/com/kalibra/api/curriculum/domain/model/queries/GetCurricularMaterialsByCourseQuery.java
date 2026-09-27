package com.kalibra.api.curriculum.domain.model.queries;

import com.kalibra.api.curriculum.domain.model.valueobjects.CourseId;
import com.kalibra.api.curriculum.domain.model.valueobjects.Pagination;

public record GetCurricularMaterialsByCourseQuery(String holderId, CourseId courseId, Pagination pagination) { }
