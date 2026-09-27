package com.kalibra.api.enrollment.infrastructure.persistence.transform;

import com.kalibra.api.enrollment.domain.model.aggregates.Invitation;
import com.kalibra.api.enrollment.infrastructure.persistence.entities.InvitationJpaEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface InvitationJpaMapper {

    InvitationJpaEntity toEntity(Invitation invitation);

    Invitation toDomain(InvitationJpaEntity entity);
}