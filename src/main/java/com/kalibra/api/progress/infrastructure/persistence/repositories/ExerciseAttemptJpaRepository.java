package com.kalibra.api.progress.infrastructure.persistence.repositories;

import com.kalibra.api.progress.infrastructure.persistence.entities.ExerciseAttemptJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExerciseAttemptJpaRepository
        extends JpaRepository<ExerciseAttemptJpaEntity, UUID> {

    List<ExerciseAttemptJpaEntity> findAllByHolderIdAndCourseIdOrderByAnsweredAtDesc(
            String holderId,
            UUID courseId
    );

    Page<ExerciseAttemptJpaEntity> findAllByHolderIdAndCourseId(
            String holderId,
            UUID courseId,
            Pageable pageable
    );

    Page<ExerciseAttemptJpaEntity> findAllByHolderIdAndCourseIdAndResult(
            String holderId,
            UUID courseId,
            String result,
            Pageable pageable
    );

    long countByHolderIdAndCourseIdAndResult(
            String holderId,
            UUID courseId,
            String result
    );

    Optional<ExerciseAttemptJpaEntity> findFirstByHolderIdAndSubtopicIdOrderByAnsweredAtDesc(
            String holderId,
            UUID subtopicId
    );

    List<ExerciseAttemptJpaEntity> findAllByCourseId(UUID courseId);
}
