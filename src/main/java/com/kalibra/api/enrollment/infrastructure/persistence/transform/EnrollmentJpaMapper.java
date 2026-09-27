package com.kalibra.api.enrollment.infrastructure.persistence.transform;

import com.kalibra.api.enrollment.domain.model.aggregates.Enrollment;
import com.kalibra.api.enrollment.domain.model.valueobjects.CourseId;
import com.kalibra.api.enrollment.domain.model.valueobjects.Email;
import com.kalibra.api.enrollment.domain.model.valueobjects.EnrollmentId;
import com.kalibra.api.enrollment.domain.model.valueobjects.InvitationId;
import com.kalibra.api.enrollment.domain.model.valueobjects.StudentId;
import com.kalibra.api.enrollment.infrastructure.persistence.entities.EnrollmentJpaEntity;
import org.mapstruct.Mapper;

import java.util.UUID;

@Mapper(componentModel = "spring")
public interface EnrollmentJpaMapper {

    EnrollmentJpaEntity toEntity(Enrollment enrollment);

    Enrollment toDomain(EnrollmentJpaEntity entity);

    // required by MapStruct 1.6: single-field VOs need explicit converters.
    default UUID map(EnrollmentId enrollmentId) {
        return enrollmentId == null ? null : enrollmentId.value();
    }

    default EnrollmentId toEnrollmentId(UUID value) {
        return value == null ? null : new EnrollmentId(value);
    }

    default UUID map(InvitationId invitationId) {
        return invitationId == null ? null : invitationId.value();
    }

    default InvitationId toInvitationId(UUID value) {
        return value == null ? null : new InvitationId(value);
    }

    default UUID map(CourseId courseId) {
        return courseId == null ? null : courseId.value();
    }

    default CourseId toCourseId(UUID value) {
        return value == null ? null : new CourseId(value);
    }

    default UUID map(StudentId studentId) {
        return studentId == null ? null : studentId.value();
    }

    default StudentId toStudentId(UUID value) {
        return value == null ? null : new StudentId(value);
    }

    default String map(Email email) {
        return email == null ? null : email.value();
    }

    default Email toEmail(String value) {
        return value == null ? null : new Email(value);
    }
}
