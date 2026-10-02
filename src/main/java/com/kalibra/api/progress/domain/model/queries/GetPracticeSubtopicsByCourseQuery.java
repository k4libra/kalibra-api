package com.kalibra.api.progress.domain.model.queries;

import com.kalibra.api.progress.domain.model.valueobjects.CourseId;

public record GetPracticeSubtopicsByCourseQuery(
        String holderId,
        CourseId courseId
) {}