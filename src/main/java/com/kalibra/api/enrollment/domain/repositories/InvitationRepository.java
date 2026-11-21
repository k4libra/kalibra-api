package com.kalibra.api.enrollment.domain.repositories;

import com.kalibra.api.enrollment.domain.model.aggregates.Invitation;
import com.kalibra.api.enrollment.domain.model.valueobjects.*;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface InvitationRepository {

    Invitation save(Invitation invitation);

    Optional<Invitation> findByIdAndHolderId(
            InvitationId id,
            String holderId
    );

    Optional<Invitation> findByIdAndStudentId(
            InvitationId id,
            StudentId studentId
    );

    List<Invitation> findAllByHolderId(
            String holderId
    );

    List<Invitation> findAllByStudentIdAndStatus(
            StudentId studentId,
            InvitationStatus status
    );

    List<Invitation> findAllByStatusAndExpiresAtBefore(
            InvitationStatus status,
            Instant now
    );

    Optional<Invitation> findByStudentIdAndCourseIdAndStatus(
            StudentId studentId,
            CourseId courseId,
            InvitationStatus status
    );
}