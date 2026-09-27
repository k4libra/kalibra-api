package com.kalibra.api.enrollment.infrastructure.persistence.transform;

import com.kalibra.api.enrollment.domain.model.aggregates.Enrollment;
import com.kalibra.api.enrollment.infrastructure.persistence.entities.EnrollmentJpaEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface EnrollmentJpaMapper {

    EnrollmentJpaEntity toEntity(Enrollment enrollment);

    Enrollment toDomain(EnrollmentJpaEntity entity);
}