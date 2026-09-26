package com.kalibra.api.profiles.infrastructure.persistence.transform;

import com.kalibra.api.profiles.domain.model.aggregates.Profile;
import com.kalibra.api.profiles.infrastructure.persistence.entities.ProfileJpaEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ProfileJpaMapper {

    ProfileJpaEntity toEntity(Profile profile);

    Profile toDomain(ProfileJpaEntity entity);
}
