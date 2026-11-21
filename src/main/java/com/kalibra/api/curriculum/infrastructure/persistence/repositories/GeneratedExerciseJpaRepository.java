package com.kalibra.api.curriculum.infrastructure.persistence.repositories;

import com.kalibra.api.curriculum.infrastructure.persistence.entities.GeneratedExerciseJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface GeneratedExerciseJpaRepository extends JpaRepository<GeneratedExerciseJpaEntity, UUID> {

    String AVAILABLE_FOR_STUDENT = """
            where e.courseId = :courseId and e.subtopicId = :subtopicId and e.difficulty = :difficulty
              and e.verification.verdict = 'APPROVED'
              and e.verification.correctnessPassed = true and e.verification.difficultyPassed = true
              and e.statement is not null and e.options is not empty
              and (:ignoreExclusions = true or e.id not in :excluded)
            """;

    @Query("select e from GeneratedExerciseJpaEntity e " + AVAILABLE_FOR_STUDENT + " order by function('random')")
    List<GeneratedExerciseJpaEntity> findAvailableForStudent(
            @Param("courseId") UUID courseId, @Param("subtopicId") UUID subtopicId,
            @Param("difficulty") String difficulty, @Param("excluded") Collection<UUID> excluded,
            @Param("ignoreExclusions") boolean ignoreExclusions, Pageable pageable);

    @Query("select count(e) from GeneratedExerciseJpaEntity e " + AVAILABLE_FOR_STUDENT)
    long countAvailableForStudent(
            @Param("courseId") UUID courseId, @Param("subtopicId") UUID subtopicId,
            @Param("difficulty") String difficulty, @Param("excluded") Collection<UUID> excluded,
            @Param("ignoreExclusions") boolean ignoreExclusions);

    List<GeneratedExerciseJpaEntity> findAllByCourseId(UUID courseId);

    Page<GeneratedExerciseJpaEntity> findAllByCourseId(UUID courseId, Pageable pageable);

    Page<GeneratedExerciseJpaEntity> findAllByCourseIdAndSubtopicId(UUID courseId, UUID subtopicId, Pageable pageable);

    List<GeneratedExerciseJpaEntity> findAllByCourseIdIn(Collection<UUID> courseIds);
}
