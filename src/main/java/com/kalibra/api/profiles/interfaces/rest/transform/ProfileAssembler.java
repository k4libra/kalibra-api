package com.kalibra.api.profiles.interfaces.rest.transform;

import com.kalibra.api.profiles.domain.model.aggregates.Profile;
import com.kalibra.api.profiles.domain.model.commands.UpdateProfileCommand;
import com.kalibra.api.profiles.interfaces.rest.resources.ProfileResource;
import com.kalibra.api.profiles.interfaces.rest.resources.UpdateProfileResource;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ProfileAssembler {

    default UpdateProfileCommand toCommand(UpdateProfileResource resource, String holderId) {
        return new UpdateProfileCommand(holderId, resource.firstName(), resource.lastName());
    }

    ProfileResource toResource(Profile profile);
}
