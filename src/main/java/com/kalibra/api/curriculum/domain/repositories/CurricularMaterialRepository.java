package com.kalibra.api.curriculum.domain.repositories;

import com.kalibra.api.curriculum.domain.model.aggregates.CurricularMaterial;
import com.kalibra.api.curriculum.domain.model.valueobjects.CourseId;
import com.kalibra.api.curriculum.domain.model.valueobjects.CurricularMaterialPage;
import com.kalibra.api.curriculum.domain.model.valueobjects.IngestionStatus;
import com.kalibra.api.curriculum.domain.model.valueobjects.MaterialId;
import com.kalibra.api.curriculum.domain.model.valueobjects.Pagination;

import java.util.List;
import java.util.Optional;

public interface CurricularMaterialRepository {

    CurricularMaterial save(CurricularMaterial material);

    Optional<CurricularMaterial> findById(MaterialId id);

    CurricularMaterialPage findAllByCourseId(CourseId courseId, Pagination pagination);

    List<CurricularMaterial> findAllByStatus(IngestionStatus status);
}
