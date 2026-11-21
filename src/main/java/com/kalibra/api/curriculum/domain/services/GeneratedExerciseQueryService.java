package com.kalibra.api.curriculum.domain.services;

import com.kalibra.api.curriculum.domain.model.aggregates.GeneratedExercise;
import com.kalibra.api.curriculum.domain.model.queries.GetExerciseCatalogByHolderIdQuery;
import com.kalibra.api.curriculum.domain.model.queries.GetGeneratedExerciseByIdQuery;
import com.kalibra.api.curriculum.domain.model.queries.GetGeneratedExercisesByCourseQuery;
import com.kalibra.api.curriculum.domain.model.queries.GetVerificationApprovalByCourseQuery;
import com.kalibra.api.curriculum.domain.model.valueobjects.CourseExerciseCatalog;
import com.kalibra.api.curriculum.domain.model.valueobjects.GeneratedExercisePage;
import com.kalibra.api.curriculum.domain.model.valueobjects.VerificationApprovalReport;

import java.util.List;
import java.util.Optional;

public interface GeneratedExerciseQueryService {

    GeneratedExercisePage handle(GetGeneratedExercisesByCourseQuery query);

    List<CourseExerciseCatalog> handle(GetExerciseCatalogByHolderIdQuery query);

    VerificationApprovalReport handle(GetVerificationApprovalByCourseQuery query);

    Optional<GeneratedExercise> handle(GetGeneratedExerciseByIdQuery query);
}
