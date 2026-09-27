package com.kalibra.api.curriculum.application.internal.queryservices;

import com.kalibra.api.curriculum.domain.exceptions.CourseNotOwnedByTeacherException;
import com.kalibra.api.curriculum.domain.model.aggregates.CurricularMaterial;
import com.kalibra.api.curriculum.domain.model.queries.GetCurricularMaterialsByCourseQuery;
import com.kalibra.api.curriculum.domain.model.queries.GetPendingIngestionMaterialsQuery;
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

    public CurricularMaterialQueryServiceImpl(CurricularMaterialRepository curricularMaterialRepository,
                                               CourseRepository courseRepository) {
        this.curricularMaterialRepository = curricularMaterialRepository;
        this.courseRepository = courseRepository;
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
