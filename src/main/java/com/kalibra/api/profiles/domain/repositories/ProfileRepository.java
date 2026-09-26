package com.kalibra.api.profiles.domain.repositories;

import com.kalibra.api.profiles.domain.model.aggregates.Profile;

import java.util.Optional;

public interface ProfileRepository {

    Profile save(Profile profile);

    Optional<Profile> findByHolderId(String holderId);
}
