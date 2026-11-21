package com.kalibra.api.curriculum.domain.services;

import com.kalibra.api.curriculum.domain.model.aggregates.CurricularMaterial;
import com.kalibra.api.curriculum.domain.model.queries.GetCurricularExtractionAvailabilityQuery;
import com.kalibra.api.curriculum.domain.model.queries.GetCurricularMaterialsByCourseQuery;
import com.kalibra.api.curriculum.domain.model.queries.GetPendingIngestionMaterialsQuery;
import com.kalibra.api.curriculum.domain.model.valueobjects.CurricularExtractionAvailability;
import com.kalibra.api.curriculum.domain.model.valueobjects.CurricularMaterialPage;

import java.util.List;

public interface CurricularMaterialQueryService {

    CurricularExtractionAvailability handle(GetCurricularExtractionAvailabilityQuery query);

    CurricularMaterialPage handle(GetCurricularMaterialsByCourseQuery query);

    List<CurricularMaterial> handle(GetPendingIngestionMaterialsQuery query);
}
