package com.kalibra.api.progress.application.internal.queryservices;

import com.kalibra.api.progress.domain.model.queries.GetPracticeSubtopicsByCourseQuery;
import com.kalibra.api.progress.domain.model.valueobjects.PracticeSubtopicView;
import com.kalibra.api.progress.domain.services.ExerciseAttemptQueryService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ExerciseAttemptQueryServiceImpl
        implements ExerciseAttemptQueryService {

    @Override
    public List<PracticeSubtopicView> handle(
            GetPracticeSubtopicsByCourseQuery query) {

        return List.of();
    }
}