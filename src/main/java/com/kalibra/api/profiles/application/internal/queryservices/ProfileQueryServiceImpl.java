package com.kalibra.api.profiles.application.internal.queryservices;

import com.kalibra.api.profiles.domain.model.aggregates.Profile;
import com.kalibra.api.profiles.domain.model.queries.GetProfileByHolderIdQuery;
import com.kalibra.api.profiles.domain.repositories.ProfileRepository;
import com.kalibra.api.profiles.domain.services.ProfileQueryService;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ProfileQueryServiceImpl implements ProfileQueryService {

    private final ProfileRepository profileRepository;

    public ProfileQueryServiceImpl(ProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    @Override
    public Optional<Profile> handle(GetProfileByHolderIdQuery query) {
        return profileRepository.findByHolderId(query.holderId());
    }
}
