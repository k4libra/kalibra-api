package com.kalibra.api.profiles.application.internal.eventhandlers;

import com.kalibra.api.iam.domain.model.events.UserRegistered;
import com.kalibra.api.profiles.domain.model.commands.CreateProfileCommand;
import com.kalibra.api.profiles.domain.services.ProfileCommandService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserRegisteredEventHandlerTest {

    @Mock
    ProfileCommandService profileCommandService;

    @InjectMocks
    UserRegisteredEventHandler handler;

    @Test
    void shouldCreateProfileWhenUserRegisters() {
        // Arrange
        var event = new UserRegistered(UUID.randomUUID(), "holder-123");

        // Act
        handler.on(event);

        // Assert
        verify(profileCommandService).handle(eq(new CreateProfileCommand("holder-123")));
    }
}
