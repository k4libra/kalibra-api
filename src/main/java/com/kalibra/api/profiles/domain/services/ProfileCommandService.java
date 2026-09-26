package com.kalibra.api.profiles.domain.services;

import com.kalibra.api.profiles.domain.model.aggregates.Profile;
import com.kalibra.api.profiles.domain.model.commands.CreateProfileCommand;
import com.kalibra.api.profiles.domain.model.commands.UpdateProfileCommand;

public interface ProfileCommandService {

    Profile handle(CreateProfileCommand command);

    Profile handle(UpdateProfileCommand command);
}
