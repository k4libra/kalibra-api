package com.kalibra.api.profiles.infrastructure.persistence.repositories;

import com.kalibra.api.profiles.domain.model.aggregates.Profile;
import com.kalibra.api.profiles.domain.repositories.ProfileRepository;
import com.kalibra.api.profiles.infrastructure.persistence.transform.ProfileJpaMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class ProfileRepositoryImpl implements ProfileRepository {

    private final ProfileJpaRepository jpaRepository;
    private final ProfileJpaMapper mapper;

    public ProfileRepositoryImpl(ProfileJpaRepository jpaRepository, ProfileJpaMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Profile save(Profile profile) {
        var entity = mapper.toEntity(profile);
        var saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Profile> findByHolderId(String holderId) {
        return jpaRepository.findByHolderId(holderId).map(mapper::toDomain);
    }
}
