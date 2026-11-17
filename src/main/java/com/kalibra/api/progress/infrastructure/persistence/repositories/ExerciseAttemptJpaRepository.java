package com.kalibra.api.progress.infrastructure.persistence.repositories;

import com.kalibra.api.progress.infrastructure.persistence.entities.ExerciseAttemptJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface ExerciseAttemptJpaRepository
        extends JpaRepository<ExerciseAttemptJpaEntity, UUID> {

    @Query("""
            select distinct a.exerciseId from ExerciseAttemptJpaEntity a
            where a.holderId = :holderId and a.subtopicId = :subtopicId
            """)
    Set<UUID> findAnsweredExerciseIdsByHolderIdAndSubtopicId(
            @Param("holderId") String holderId, @Param("subtopicId") UUID subtopicId);

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

    Optional<ExerciseAttemptJpaEntity> findFirstByHolderIdAndCourseIdOrderByAnsweredAtDesc(String holderId, UUID courseId);

    List<ExerciseAttemptJpaEntity> findAllByCourseId(UUID courseId);
}
