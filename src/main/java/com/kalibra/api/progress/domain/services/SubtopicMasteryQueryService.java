package com.kalibra.api.progress.domain.services;

import com.kalibra.api.progress.domain.model.queries.ExportCourseIndicatorsQuery;
import com.kalibra.api.progress.domain.model.queries.GetCourseIndicatorsQuery;
import com.kalibra.api.progress.domain.model.queries.GetIndicatorGuideQuery;
import com.kalibra.api.progress.domain.model.queries.GetMasteryGapMapByCourseQuery;
import com.kalibra.api.progress.domain.model.queries.GetPracticeSubtopicsByCourseQuery;
import com.kalibra.api.progress.domain.model.queries.GetStudentProgressByCourseQuery;
import com.kalibra.api.progress.domain.model.queries.GetStudentProgressForTeacherQuery;
import com.kalibra.api.progress.domain.model.valueobjects.CourseIndicatorsReport;
import com.kalibra.api.progress.domain.model.valueobjects.IndicatorGuide;
import com.kalibra.api.progress.domain.model.valueobjects.IndicatorsCsvExport;
import com.kalibra.api.progress.domain.model.valueobjects.MasteryGapMap;
import com.kalibra.api.progress.domain.model.valueobjects.PracticeSubtopicView;
import com.kalibra.api.progress.domain.model.valueobjects.StudentProgressReport;

import java.util.List;

public interface SubtopicMasteryQueryService {

    List<PracticeSubtopicView> handle(GetPracticeSubtopicsByCourseQuery query);

    StudentProgressReport handle(GetStudentProgressByCourseQuery query);

    StudentProgressReport handle(GetStudentProgressForTeacherQuery query);

    MasteryGapMap handle(GetMasteryGapMapByCourseQuery query);

    CourseIndicatorsReport handle(GetCourseIndicatorsQuery query);

    IndicatorsCsvExport handle(ExportCourseIndicatorsQuery query);

    IndicatorGuide handle(GetIndicatorGuideQuery query);
}
