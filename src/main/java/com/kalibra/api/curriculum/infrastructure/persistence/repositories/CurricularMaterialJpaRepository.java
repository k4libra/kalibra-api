package com.kalibra.api.curriculum.infrastructure.persistence.repositories;

import com.kalibra.api.curriculum.infrastructure.persistence.entities.CurricularMaterialJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CurricularMaterialJpaRepository extends JpaRepository<CurricularMaterialJpaEntity, UUID> {

    Page<CurricularMaterialJpaEntity> findAllByCourseId(UUID courseId, Pageable pageable);

    List<CurricularMaterialJpaEntity> findAllByStatus(String status);
}
