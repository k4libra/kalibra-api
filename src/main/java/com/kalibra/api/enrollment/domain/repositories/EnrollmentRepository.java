package com.kalibra.api.enrollment.domain.repositories;

import com.kalibra.api.enrollment.domain.model.aggregates.Enrollment;
import com.kalibra.api.enrollment.domain.model.valueobjects.*;

import java.util.List;
import java.util.Optional;

public interface EnrollmentRepository {

    Enrollment save(Enrollment enrollment);

    List<Enrollment> findAllByCourseId(
            CourseId courseId
    );

    List<Enrollment> findAllByCourseIdIn(
            List<CourseId> courseIds
    );

    Optional<Enrollment> findByStudentIdAndCourseId(
            StudentId studentId,
            CourseId courseId
    );

    boolean existsByInvitationId(
            InvitationId invitationId
    );
}