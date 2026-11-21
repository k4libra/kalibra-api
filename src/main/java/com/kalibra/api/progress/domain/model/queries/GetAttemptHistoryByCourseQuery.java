package com.kalibra.api.progress.domain.model.queries;

import com.kalibra.api.progress.domain.model.valueobjects.AttemptResultFilter;
import com.kalibra.api.progress.domain.model.valueobjects.CourseId;
import com.kalibra.api.progress.domain.model.valueobjects.Pagination;

public record GetAttemptHistoryByCourseQuery(
        String holderId,
        CourseId courseId,
        AttemptResultFilter filter,
        Pagination pagination
) {
}
