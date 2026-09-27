package com.kalibra.api.iam.application.internal.commandservices;

import com.kalibra.api.iam.application.internal.outboundservices.hashing.HashingService;
import com.kalibra.api.iam.domain.exceptions.EmailAlreadyRegisteredException;
import com.kalibra.api.iam.domain.model.aggregates.User;
import com.kalibra.api.iam.domain.model.commands.SignInCommand;
import com.kalibra.api.iam.domain.model.commands.SignUpCommand;
import com.kalibra.api.iam.domain.model.events.UserRegistered;
import com.kalibra.api.iam.domain.model.valueobjects.ClientApplication;
import com.kalibra.api.iam.domain.model.valueobjects.Email;
import com.kalibra.api.iam.domain.model.valueobjects.HashedPassword;
import com.kalibra.api.iam.domain.model.valueobjects.Role;
import com.kalibra.api.iam.domain.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserCommandServiceImplTest {

    private static final Email EMAIL = new Email("ada@kalibra.com");

    @Mock
    UserRepository userRepository;

    @Mock
    HashingService hashingService;

    @Mock
    ApplicationEventPublisher eventPublisher;

    @InjectMocks
    UserCommandServiceImpl service;

    @Test
    void shouldRegisterUserWithRoleOfTheClientApplicationAndPublishEvent() {
        // Arrange
        var command = new SignUpCommand(EMAIL, "secret-123", ClientApplication.MOBILE_APP);
        var userCaptor = ArgumentCaptor.forClass(User.class);
        var eventCaptor = ArgumentCaptor.forClass(UserRegistered.class);
        when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(hashingService.hash("secret-123")).thenReturn("hashed");
        when(userRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        var result = service.handle(command);

        // Assert
        verify(userRepository).save(userCaptor.capture());
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(result).isPresent();
        assertThat(userCaptor.getValue().getHashedPassword()).isEqualTo(new HashedPassword("hashed"));
        assertThat(userCaptor.getValue().getRoles()).containsExactlyInAnyOrder(Role.REGISTERED_USER, Role.STUDENT);
        assertThat(eventCaptor.getValue().userId()).isEqualTo(userCaptor.getValue().getId());
    }

    @Test
    void shouldThrowWhenEmailIsAlreadyRegistered() {
        // Arrange
        var command = new SignUpCommand(EMAIL, "secret-123", ClientApplication.WEB_PLATFORM);
        when(userRepository.existsByEmail(EMAIL)).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> service.handle(command)).isInstanceOf(EmailAlreadyRegisteredException.class);
        verify(userRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void shouldThrowEmailAlreadyRegisteredWhenAConcurrentSignUpWinsTheRace() {
        // Arrange
        var command = new SignUpCommand(EMAIL, "secret-123", ClientApplication.MOBILE_APP);
        when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(hashingService.hash("secret-123")).thenReturn("hashed");
        when(userRepository.save(any())).thenThrow(new DataIntegrityViolationException("users_email_key"));

        // Act & Assert
        assertThatThrownBy(() -> service.handle(command)).isInstanceOf(EmailAlreadyRegisteredException.class);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void shouldReturnUserWhenCredentialsMatch() {
        // Arrange
        var user = User.register(EMAIL, new HashedPassword("hashed"), ClientApplication.MOBILE_APP);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(hashingService.matches("secret-123", "hashed")).thenReturn(true);

        // Act & Assert
        assertThat(service.handle(new SignInCommand(EMAIL, "secret-123"))).contains(user);
    }

    @Test
    void shouldReturnEmptyWhenPasswordDoesNotMatch() {
        // Arrange
        var user = User.register(EMAIL, new HashedPassword("hashed"), ClientApplication.MOBILE_APP);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(hashingService.matches("wrong", "hashed")).thenReturn(false);

        // Act & Assert
        assertThat(service.handle(new SignInCommand(EMAIL, "wrong"))).isEmpty();
    }

    @Test
    void shouldReturnEmptyWhenEmailIsUnknown() {
        // Arrange
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        // Act & Assert
        assertThat(service.handle(new SignInCommand(EMAIL, "secret-123"))).isEmpty();
    }
}
