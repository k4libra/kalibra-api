package com.kalibra.api.progress.domain.services;

import com.kalibra.api.progress.domain.model.queries.GetPracticeSubtopicsByCourseQuery;
import com.kalibra.api.progress.domain.model.queries.GetStudentProgressByCourseQuery;
import com.kalibra.api.progress.domain.model.valueobjects.PracticeSubtopicView;
import com.kalibra.api.progress.domain.model.valueobjects.StudentProgressReport;

import java.util.List;

public interface SubtopicMasteryQueryService {

    List<PracticeSubtopicView> handle(
            GetPracticeSubtopicsByCourseQuery query
    );

    StudentProgressReport handle(
            GetStudentProgressByCourseQuery query
    );
}