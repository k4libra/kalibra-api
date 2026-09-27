package com.kalibra.api.enrollment.infrastructure.persistence.repositories;

import com.kalibra.api.enrollment.infrastructure.persistence.entities.EnrollmentJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EnrollmentJpaRepository
        extends JpaRepository<EnrollmentJpaEntity, UUID> {

    List<EnrollmentJpaEntity> findAllByCourseId(UUID courseId);

    List<EnrollmentJpaEntity> findAllByCourseIdIn(List<UUID> courseIds);

    Optional<EnrollmentJpaEntity> findByStudentIdAndCourseId(
            UUID studentId,
            UUID courseId
    );

    boolean existsByInvitationId(UUID invitationId);
}