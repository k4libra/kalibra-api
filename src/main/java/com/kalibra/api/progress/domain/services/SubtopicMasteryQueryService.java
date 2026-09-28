package com.kalibra.api.progress.domain.services;

import com.kalibra.api.progress.domain.model.queries.GetStudentProgressByCourseQuery;
import com.kalibra.api.progress.domain.model.valueobjects.StudentProgressReport;

public interface SubtopicMasteryQueryService {

    StudentProgressReport handle(
            GetStudentProgressByCourseQuery query
    );
}