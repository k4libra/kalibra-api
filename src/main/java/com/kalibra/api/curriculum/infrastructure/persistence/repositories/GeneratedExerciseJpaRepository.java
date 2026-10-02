package com.kalibra.api.curriculum.infrastructure.persistence.repositories;

import com.kalibra.api.curriculum.infrastructure.persistence.entities.GeneratedExerciseJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface GeneratedExerciseJpaRepository extends JpaRepository<GeneratedExerciseJpaEntity, UUID> {

    List<GeneratedExerciseJpaEntity> findAllByCourseId(UUID courseId);

    Page<GeneratedExerciseJpaEntity> findAllByCourseId(UUID courseId, Pageable pageable);

    Page<GeneratedExerciseJpaEntity> findAllByCourseIdAndSubtopicId(UUID courseId, UUID subtopicId, Pageable pageable);

    List<GeneratedExerciseJpaEntity> findAllByCourseIdIn(Collection<UUID> courseIds);
}
