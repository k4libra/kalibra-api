package com.kalibra.api.enrollment.infrastructure.persistence.repositories;

import com.kalibra.api.enrollment.domain.model.aggregates.Invitation;
import com.kalibra.api.enrollment.domain.model.valueobjects.*;
import com.kalibra.api.enrollment.domain.repositories.InvitationRepository;
import com.kalibra.api.enrollment.infrastructure.persistence.transform.InvitationJpaMapper;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public class InvitationRepositoryImpl implements InvitationRepository {

    private final InvitationJpaRepository jpaRepository;
    private final InvitationJpaMapper mapper;

    public InvitationRepositoryImpl(
            InvitationJpaRepository jpaRepository,
            InvitationJpaMapper mapper
    ) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Invitation save(Invitation invitation) {
        return mapper.toDomain(
                jpaRepository.save(mapper.toEntity(invitation))
        );
    }

    @Override
    public Optional<Invitation> findByIdAndHolderId(
            InvitationId id,
            String holderId
    ) {
        return jpaRepository
                .findByIdAndHolderId(id.value(), holderId)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<Invitation> findByIdAndStudentId(
            InvitationId id,
            StudentId studentId
    ) {
        return jpaRepository
                .findByIdAndStudentId(id.value(), studentId.value())
                .map(mapper::toDomain);
    }

    @Override
    public List<Invitation> findAllByHolderId(String holderId) {
        return jpaRepository.findAllByHolderId(holderId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Invitation> findAllByStudentIdAndStatus(
            StudentId studentId,
            InvitationStatus status
    ) {
        return jpaRepository
                .findAllByStudentIdAndStatus(
                        studentId.value(),
                        status.name()
                )
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Invitation> findAllByStatusAndExpiresAtBefore(
            InvitationStatus status,
            Instant now
    ) {
        return jpaRepository
                .findAllByStatusAndExpiresAtBefore(
                        status.name(),
                        now
                )
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Invitation> findByStudentIdAndCourseIdAndStatus(
            StudentId studentId,
            CourseId courseId,
            InvitationStatus status
    ) {
        return jpaRepository
                .findByStudentIdAndCourseIdAndStatus(
                        studentId.value(),
                        courseId.value(),
                        status.name()
                )
                .map(mapper::toDomain);
    }
}