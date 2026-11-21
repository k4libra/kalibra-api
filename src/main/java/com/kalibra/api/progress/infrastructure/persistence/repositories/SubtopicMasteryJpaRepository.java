package com.kalibra.api.progress.infrastructure.persistence.repositories;

import com.kalibra.api.progress.infrastructure.persistence.entities.SubtopicMasteryJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubtopicMasteryJpaRepository
        extends JpaRepository<SubtopicMasteryJpaEntity, UUID> {

    Optional<SubtopicMasteryJpaEntity> findByHolderIdAndSubtopicId(
            String holderId,
            UUID subtopicId
    );

    List<SubtopicMasteryJpaEntity> findAllByHolderIdAndCourseId(
            String holderId,
            UUID courseId
    );

    List<SubtopicMasteryJpaEntity> findAllByCourseId(UUID courseId);
}