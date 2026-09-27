package com.kalibra.api.enrollment.infrastructure.persistence.repositories;

import com.kalibra.api.enrollment.infrastructure.persistence.entities.InvitationJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvitationJpaRepository
        extends JpaRepository<InvitationJpaEntity, UUID> {

    Optional<InvitationJpaEntity> findByIdAndHolderId(UUID id, String holderId);

    Optional<InvitationJpaEntity> findByIdAndStudentId(UUID id, UUID studentId);

    List<InvitationJpaEntity> findAllByHolderId(String holderId);

    List<InvitationJpaEntity> findAllByStudentIdAndStatus(UUID studentId, String status);

    List<InvitationJpaEntity> findAllByStatusAndExpiresAtBefore(
            String status,
            Instant now
    );

    Optional<InvitationJpaEntity> findByStudentIdAndCourseIdAndStatus(
            UUID studentId,
            UUID courseId,
            String status
    );
}