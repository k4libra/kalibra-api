package com.kalibra.api.progress.application.internal.queryservices;

import com.kalibra.api.progress.domain.model.queries.GetStudentProgressByCourseQuery;
import com.kalibra.api.progress.domain.model.valueobjects.StudentProgressReport;
import com.kalibra.api.progress.domain.services.SubtopicMasteryQueryService;
import org.springframework.stereotype.Service;

@Service
public class SubtopicMasteryQueryServiceImpl
        implements SubtopicMasteryQueryService {

    @Override
    public StudentProgressReport handle(
            GetStudentProgressByCourseQuery query) {

        return null;
    }
}