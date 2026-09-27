package com.kalibra.api.enrollment.infrastructure.persistence.transform;

import com.kalibra.api.enrollment.domain.model.aggregates.Invitation;
import com.kalibra.api.enrollment.domain.model.valueobjects.CourseId;
import com.kalibra.api.enrollment.domain.model.valueobjects.Email;
import com.kalibra.api.enrollment.domain.model.valueobjects.InvitationId;
import com.kalibra.api.enrollment.domain.model.valueobjects.InvitationStatus;
import com.kalibra.api.enrollment.domain.model.valueobjects.InvitationValidity;
import com.kalibra.api.enrollment.domain.model.valueobjects.StudentId;
import com.kalibra.api.enrollment.infrastructure.persistence.entities.InvitationJpaEntity;
import com.kalibra.api.enrollment.infrastructure.persistence.entities.InvitationValidityEmbeddable;
import org.mapstruct.Mapper;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface InvitationJpaMapper {

    InvitationJpaEntity toEntity(Invitation invitation);

    Invitation toDomain(InvitationJpaEntity entity);

    InvitationValidityEmbeddable toEmbeddable(InvitationValidity validity);

    InvitationValidity toInvitationValidity(InvitationValidityEmbeddable embeddable);

    // required by MapStruct 1.6: Optional fields and single-field VOs need explicit converters.
    default Instant fromOptionalRespondedAt(Optional<Instant> respondedAt) {
        return respondedAt == null ? null : respondedAt.orElse(null);
    }

    default Optional<Instant> toOptionalRespondedAt(Instant respondedAt) {
        return Optional.ofNullable(respondedAt);
    }

    default String map(InvitationStatus status) {
        return status == null ? null : status.name();
    }

    default InvitationStatus toInvitationStatus(String value) {
        return value == null ? null : InvitationStatus.valueOf(value);
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
