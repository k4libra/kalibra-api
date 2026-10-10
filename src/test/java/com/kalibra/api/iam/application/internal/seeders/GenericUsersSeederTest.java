package com.kalibra.api.iam.application.internal.seeders;

import com.kalibra.api.iam.application.internal.outboundservices.hashing.HashingService;
import com.kalibra.api.iam.domain.model.aggregates.User;
import com.kalibra.api.iam.domain.model.valueobjects.Email;
import com.kalibra.api.iam.domain.model.valueobjects.Role;
import com.kalibra.api.iam.domain.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GenericUsersSeederTest {

    @Mock
    UserRepository userRepository;

    @Mock
    HashingService hashingService;

    @InjectMocks
    GenericUsersSeeder seeder;

    @Test
    void shouldCreateTwoTeachersThreeStudentsAndOneAdministrator() {
        when(userRepository.existsByEmail(any(Email.class))).thenReturn(false);
        when(hashingService.hash(anyString())).thenReturn("hashed");

        seeder.run(null);

        var captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(6)).save(captor.capture());
        var users = captor.getAllValues();
        assertThat(users.stream().filter(user -> user.hasRole(Role.TEACHER))).hasSize(2);
        assertThat(users.stream().filter(user -> user.hasRole(Role.STUDENT))).hasSize(3);
        assertThat(users.stream().filter(user -> user.hasRole(Role.ADMINISTRATOR)))
                .singleElement()
                .satisfies(admin -> assertThat(admin.getEmail().value()).isEqualTo("admin.test1@upc.edu.pe"));
        verify(hashingService).hash("@profesortest1");
        verify(hashingService).hash("@estudiantetest3");
    }

    @Test
    void shouldSkipAccountsThatAlreadyExist() {
        when(userRepository.existsByEmail(any(Email.class))).thenReturn(true);

        seeder.run(null);

        verify(userRepository, never()).save(any(User.class));
    }
}
