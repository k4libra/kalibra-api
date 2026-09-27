package com.kalibra.api.iam.application.internal.queryservices;

import com.kalibra.api.iam.domain.model.aggregates.User;
import com.kalibra.api.iam.domain.model.queries.GetUserByEmailQuery;
import com.kalibra.api.iam.domain.model.queries.GetUserByIdQuery;
import com.kalibra.api.iam.domain.model.valueobjects.ClientApplication;
import com.kalibra.api.iam.domain.model.valueobjects.Email;
import com.kalibra.api.iam.domain.model.valueobjects.HashedPassword;
import com.kalibra.api.iam.domain.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserQueryServiceImplTest {

    private static final Email EMAIL = new Email("ada@kalibra.com");

    @Mock
    UserRepository userRepository;

    @InjectMocks
    UserQueryServiceImpl service;

    @Test
    void shouldReturnUserWhenFoundById() {
        // Arrange
        var user = User.register(EMAIL, new HashedPassword("hashed"), ClientApplication.MOBILE_APP);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        // Act & Assert
        assertThat(service.handle(new GetUserByIdQuery(user.getId()))).contains(user);
    }

    @Test
    void shouldReturnEmptyWhenIdIsUnknown() {
        // Arrange
        var id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        // Act & Assert
        assertThat(service.handle(new GetUserByIdQuery(id))).isEmpty();
    }

    @Test
    void shouldReturnUserWhenFoundByEmail() {
        // Arrange
        var user = User.register(EMAIL, new HashedPassword("hashed"), ClientApplication.WEB_PLATFORM);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        // Act & Assert
        assertThat(service.handle(new GetUserByEmailQuery(EMAIL))).contains(user);
    }
}
