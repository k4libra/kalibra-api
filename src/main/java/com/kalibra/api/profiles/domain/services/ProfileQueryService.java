package com.kalibra.api.profiles.domain.services;

import com.kalibra.api.profiles.domain.model.aggregates.Profile;
import com.kalibra.api.profiles.domain.model.queries.GetProfileByHolderIdQuery;

import java.util.Optional;

public interface ProfileQueryService {

    Optional<Profile> handle(GetProfileByHolderIdQuery query);
}
