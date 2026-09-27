package com.kalibra.api.enrollment.infrastructure.persistence.repositories;

import com.kalibra.api.enrollment.domain.model.aggregates.Enrollment;
import com.kalibra.api.enrollment.domain.model.valueobjects.CourseId;
import com.kalibra.api.enrollment.domain.model.valueobjects.InvitationId;
import com.kalibra.api.enrollment.domain.model.valueobjects.StudentId;
import com.kalibra.api.enrollment.domain.repositories.EnrollmentRepository;
import com.kalibra.api.enrollment.infrastructure.persistence.transform.EnrollmentJpaMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class EnrollmentRepositoryImpl implements EnrollmentRepository {

    private final EnrollmentJpaRepository jpaRepository;
    private final EnrollmentJpaMapper mapper;

    public EnrollmentRepositoryImpl(
            EnrollmentJpaRepository jpaRepository,
            EnrollmentJpaMapper mapper
    ) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Enrollment save(Enrollment enrollment) {
        // flush: unique indexes must fail here, inside the caller's transaction, not at commit.
        return mapper.toDomain(
                jpaRepository.saveAndFlush(mapper.toEntity(enrollment))
        );
    }

    @Override
    public List<Enrollment> findAllByCourseId(CourseId courseId) {
        return jpaRepository.findAllByCourseId(courseId.value())
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Enrollment> findAllByCourseIdIn(List<CourseId> courseIds) {
        var ids = courseIds.stream()
                .map(CourseId::value)
                .toList();

        return jpaRepository.findAllByCourseIdIn(ids)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Enrollment> findByStudentIdAndCourseId(
            StudentId studentId,
            CourseId courseId
    ) {
        return jpaRepository
                .findByStudentIdAndCourseId(
                        studentId.value(),
                        courseId.value()
                )
                .map(mapper::toDomain);
    }

    @Override
    public boolean existsByInvitationId(InvitationId invitationId) {
        return jpaRepository.existsByInvitationId(invitationId.value());
    }
}