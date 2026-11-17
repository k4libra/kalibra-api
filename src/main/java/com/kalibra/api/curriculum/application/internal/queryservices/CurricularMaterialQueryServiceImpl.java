package com.kalibra.api.curriculum.application.internal.queryservices;

import com.kalibra.api.curriculum.application.internal.outboundservices.acl.ExternalCurricularExtractionService;
import com.kalibra.api.curriculum.domain.exceptions.CourseNotOwnedByTeacherException;
import com.kalibra.api.curriculum.domain.model.aggregates.CurricularMaterial;
import com.kalibra.api.curriculum.domain.model.queries.GetCurricularExtractionAvailabilityQuery;
import com.kalibra.api.curriculum.domain.model.queries.GetCurricularMaterialsByCourseQuery;
import com.kalibra.api.curriculum.domain.model.queries.GetPendingIngestionMaterialsQuery;
import com.kalibra.api.curriculum.domain.model.valueobjects.CurricularExtractionAvailability;
import com.kalibra.api.curriculum.domain.model.valueobjects.CurricularMaterialPage;
import com.kalibra.api.curriculum.domain.model.valueobjects.IngestionStatus;
import com.kalibra.api.curriculum.domain.repositories.CourseRepository;
import com.kalibra.api.curriculum.domain.repositories.CurricularMaterialRepository;
import com.kalibra.api.curriculum.domain.services.CurricularMaterialQueryService;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CurricularMaterialQueryServiceImpl implements CurricularMaterialQueryService {

    private final CurricularMaterialRepository curricularMaterialRepository;
    private final CourseRepository courseRepository;
    private final ExternalCurricularExtractionService externalCurricularExtractionService;

    public CurricularMaterialQueryServiceImpl(CurricularMaterialRepository curricularMaterialRepository,
                                               CourseRepository courseRepository,
                                               ExternalCurricularExtractionService externalCurricularExtractionService) {
        this.curricularMaterialRepository = curricularMaterialRepository;
        this.courseRepository = courseRepository;
        this.externalCurricularExtractionService = externalCurricularExtractionService;
    }

    @Override
    public CurricularExtractionAvailability handle(GetCurricularExtractionAvailabilityQuery query) {
        return new CurricularExtractionAvailability(externalCurricularExtractionService.isCurricularExtractionAvailable());
    }

    @Override
    public CurricularMaterialPage handle(GetCurricularMaterialsByCourseQuery query) {
        courseRepository.findByIdAndHolderId(query.courseId(), query.holderId())
                .orElseThrow(() -> new CourseNotOwnedByTeacherException(query.courseId().value()));
        return curricularMaterialRepository.findAllByCourseId(query.courseId(), query.pagination());
    }

    @Override
    public List<CurricularMaterial> handle(GetPendingIngestionMaterialsQuery query) {
        return curricularMaterialRepository.findAllByStatus(IngestionStatus.PENDING_INGESTION);
    }
}
