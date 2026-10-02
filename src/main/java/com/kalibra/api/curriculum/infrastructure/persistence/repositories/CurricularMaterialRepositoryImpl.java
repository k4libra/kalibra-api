package com.kalibra.api.curriculum.infrastructure.persistence.repositories;

import com.kalibra.api.curriculum.domain.model.aggregates.CurricularMaterial;
import com.kalibra.api.curriculum.domain.model.valueobjects.CourseId;
import com.kalibra.api.curriculum.domain.model.valueobjects.CurricularMaterialPage;
import com.kalibra.api.curriculum.domain.model.valueobjects.IngestionStatus;
import com.kalibra.api.curriculum.domain.model.valueobjects.MaterialId;
import com.kalibra.api.curriculum.domain.model.valueobjects.Pagination;
import com.kalibra.api.curriculum.domain.model.valueobjects.SubtopicId;
import com.kalibra.api.curriculum.domain.repositories.CurricularMaterialRepository;
import com.kalibra.api.curriculum.infrastructure.persistence.transform.CurricularMaterialJpaMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class CurricularMaterialRepositoryImpl implements CurricularMaterialRepository {

    private final CurricularMaterialJpaRepository jpaRepository;
    private final CurricularMaterialJpaMapper mapper;

    public CurricularMaterialRepositoryImpl(CurricularMaterialJpaRepository jpaRepository,
                                             CurricularMaterialJpaMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public CurricularMaterial save(CurricularMaterial material) {
        var entity = mapper.toEntity(material);
        var saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<CurricularMaterial> findById(MaterialId id) {
        return jpaRepository.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    public CurricularMaterialPage findAllByCourseId(CourseId courseId, Pagination pagination) {
        var pageable = PageRequest.of(pagination.page(), pagination.size(), Sort.by(Sort.Direction.DESC, "uploadedAt"));
        var page = jpaRepository.findAllByCourseId(courseId.value(), pageable);
        var items = page.getContent().stream().map(mapper::toDomain).toList();
        return new CurricularMaterialPage(items, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    @Override
    public List<CurricularMaterial> findAllByStatus(IngestionStatus status) {
        return jpaRepository.findAllByStatus(status.name()).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<CurricularMaterial> findAllByCourseIdAndSubtopicIdAndStatus(CourseId courseId, SubtopicId subtopicId, IngestionStatus status) {
        return jpaRepository.findAllByCourseIdAndSubtopicIdAndStatus(courseId.value(), subtopicId.value(), status.name()).stream()
                .map(mapper::toDomain)
                .toList();
    }
}
