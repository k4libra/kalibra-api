package com.kalibra.api.curriculum.infrastructure.persistence.repositories;

import com.kalibra.api.curriculum.infrastructure.persistence.entities.CurricularMaterialJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface CurricularMaterialJpaRepository extends JpaRepository<CurricularMaterialJpaEntity, UUID> {

    Page<CurricularMaterialJpaEntity> findAllByCourseId(UUID courseId, Pageable pageable);

    List<CurricularMaterialJpaEntity> findAllByStatus(String status);

    @Query("""
            select material from CurricularMaterialJpaEntity material
            where material.courseId = :courseId
              and :subtopicId member of material.subtopicIds
              and material.status = :status
            order by material.uploadedAt asc
            """)
    List<CurricularMaterialJpaEntity> findAllByCourseIdAndSubtopicIdAndStatus(@Param("courseId") UUID courseId,
                                                                             @Param("subtopicId") UUID subtopicId,
                                                                             @Param("status") String status);
}
