package com.kalibra.api.profiles.interfaces.rest;

import com.kalibra.api.profiles.domain.model.commands.CreateProfileCommand;
import com.kalibra.api.profiles.domain.model.queries.GetProfileByHolderIdQuery;
import com.kalibra.api.profiles.domain.services.ProfileCommandService;
import com.kalibra.api.profiles.domain.services.ProfileQueryService;
import com.kalibra.api.profiles.interfaces.rest.resources.ProfileResource;
import com.kalibra.api.profiles.interfaces.rest.resources.UpdateProfileResource;
import com.kalibra.api.profiles.interfaces.rest.transform.ProfileAssembler;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/profiles")
public class ProfilesController {

    private final ProfileCommandService commandService;
    private final ProfileQueryService queryService;
    private final ProfileAssembler assembler;

    public ProfilesController(ProfileCommandService commandService,
                               ProfileQueryService queryService,
                               ProfileAssembler assembler) {
        this.commandService = commandService;
        this.queryService = queryService;
        this.assembler = assembler;
    }

    // Self-heals: if UserRegisteredEventHandler hasn't created the profile yet
    // (or failed after the iam commit), it's created here instead of 404-ing.
    @GetMapping("/me")
    public ResponseEntity<ProfileResource> getMyProfile(Authentication authentication) {
        var holderId = authentication.getName();
        var profile = queryService.handle(new GetProfileByHolderIdQuery(holderId))
                .orElseGet(() -> commandService.handle(new CreateProfileCommand(holderId)));
        return ResponseEntity.ok(assembler.toResource(profile));
    }

    @PutMapping("/me")
    public ResponseEntity<ProfileResource> updateMyProfile(Authentication authentication,
                                                             @Valid @RequestBody UpdateProfileResource resource) {
        var holderId = authentication.getName();
        var command = assembler.toCommand(resource, holderId);
        var profile = commandService.handle(command);
        return ResponseEntity.ok(assembler.toResource(profile));
    }
}
