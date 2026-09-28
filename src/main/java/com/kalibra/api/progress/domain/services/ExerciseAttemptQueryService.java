package com.kalibra.api.progress.domain.services;

import com.kalibra.api.progress.domain.model.queries.GetPracticeSubtopicsByCourseQuery;
import com.kalibra.api.progress.domain.model.valueobjects.PracticeSubtopicView;

import java.util.List;

public interface ExerciseAttemptQueryService {

    List<PracticeSubtopicView> handle(
            GetPracticeSubtopicsByCourseQuery query
    );
}