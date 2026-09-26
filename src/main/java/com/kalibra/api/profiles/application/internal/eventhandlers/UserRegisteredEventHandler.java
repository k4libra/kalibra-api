package com.kalibra.api.profiles.application.internal.eventhandlers;

import com.kalibra.api.iam.domain.model.events.UserRegistered;
import com.kalibra.api.profiles.domain.model.commands.CreateProfileCommand;
import com.kalibra.api.profiles.domain.services.ProfileCommandService;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.stereotype.Service;

@Service
public class UserRegisteredEventHandler {

    private final ProfileCommandService profileCommandService;

    public UserRegisteredEventHandler(ProfileCommandService profileCommandService) {
        this.profileCommandService = profileCommandService;
    }

    // AFTER_COMMIT: the profiles module reacts only once sign-up is a committed fact,
    // so a failure here can never roll back the iam transaction. If this handler
    // fails, ProfilesController self-heals by creating the profile on first read/update
    // (see getMyProfile / ProfileCommandServiceImpl.handle(UpdateProfileCommand)).
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(UserRegistered event) {
        profileCommandService.handle(new CreateProfileCommand(event.holderId()));
    }
}
