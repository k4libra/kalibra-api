package com.kalibra.api.profiles.application.internal.commandservices;

import com.kalibra.api.profiles.domain.model.aggregates.Profile;
import com.kalibra.api.profiles.domain.model.commands.CreateProfileCommand;
import com.kalibra.api.profiles.domain.model.commands.UpdateProfileCommand;
import com.kalibra.api.profiles.domain.repositories.ProfileRepository;
import com.kalibra.api.profiles.domain.services.ProfileCommandService;
import org.springframework.stereotype.Service;

@Service
public class ProfileCommandServiceImpl implements ProfileCommandService {

    private final ProfileRepository profileRepository;

    public ProfileCommandServiceImpl(ProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    @Override
    public Profile handle(CreateProfileCommand command) {
        var profile = Profile.createEmptyFor(command.holderId());
        return profileRepository.save(profile);
    }

    // Self-heals: if the UserRegistered handler hasn't created the profile yet
    // (or failed after the iam commit), updating still creates it here instead of 404-ing.
    @Override
    public Profile handle(UpdateProfileCommand command) {
        var profile = profileRepository.findByHolderId(command.holderId())
                .orElseGet(() -> Profile.createEmptyFor(command.holderId()));
        profile.updatePersonalData(command.firstName(), command.lastName());
        return profileRepository.save(profile);
    }
}
