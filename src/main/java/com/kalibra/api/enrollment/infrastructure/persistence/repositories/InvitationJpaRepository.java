package com.kalibra.api.enrollment.infrastructure.persistence.repositories;

import com.kalibra.api.enrollment.infrastructure.persistence.entities.InvitationJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    // expiresAt lives in the embedded validity, so the query cannot be derived from the method name.
    @Query("select i from InvitationJpaEntity i where i.status = :status and i.validity.expiresAt <= :now")
    List<InvitationJpaEntity> findAllByStatusAndExpiresAtBefore(
            @Param("status") String status,
            @Param("now") Instant now
    );

    Optional<InvitationJpaEntity> findByStudentIdAndCourseIdAndStatus(
            UUID studentId,
            UUID courseId,
            String status
    );
}