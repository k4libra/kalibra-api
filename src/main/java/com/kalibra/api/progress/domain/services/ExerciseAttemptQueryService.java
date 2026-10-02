package com.kalibra.api.progress.domain.services;

import com.kalibra.api.progress.domain.model.queries.GetAttemptHistoryByCourseQuery;
import com.kalibra.api.progress.domain.model.valueobjects.AttemptHistoryView;

public interface ExerciseAttemptQueryService {

    AttemptHistoryView handle(GetAttemptHistoryByCourseQuery query);
}
