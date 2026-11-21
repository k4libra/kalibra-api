package com.kalibra.api.progress.domain.services;

import com.kalibra.api.progress.domain.model.queries.GetCriticalSubtopicsQuery;
import com.kalibra.api.progress.domain.model.queries.GetInstitutionalIndicatorsQuery;
import com.kalibra.api.progress.domain.model.valueobjects.CriticalSubtopicLine;
import com.kalibra.api.progress.domain.model.valueobjects.InstitutionalIndicators;

import java.util.List;

public interface InstitutionalIndicatorsQueryService {

    InstitutionalIndicators handle(GetInstitutionalIndicatorsQuery query);

    List<CriticalSubtopicLine> handle(GetCriticalSubtopicsQuery query);
}
