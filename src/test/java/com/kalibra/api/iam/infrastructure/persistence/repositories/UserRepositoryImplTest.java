package com.kalibra.api.iam.infrastructure.persistence.repositories;

import com.kalibra.api.iam.domain.model.aggregates.User;
import com.kalibra.api.iam.domain.model.valueobjects.ClientApplication;
import com.kalibra.api.iam.domain.model.valueobjects.Email;
import com.kalibra.api.iam.domain.model.valueobjects.HashedPassword;
import com.kalibra.api.iam.infrastructure.persistence.entities.UserJpaEntity;
import com.kalibra.api.iam.infrastructure.persistence.transform.UserJpaMapper;
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
class UserRepositoryImplTest {

    @Mock
    UserJpaRepository jpaRepository;

    @Mock
    UserJpaMapper mapper;

    @InjectMocks
    UserRepositoryImpl repository;

    @Test
    void shouldReturnUserWhenFoundById() {
        // Arrange
        var user = User.register(new Email("ada@kalibra.com"), new HashedPassword("hashed"), ClientApplication.MOBILE_APP);
        var entity = new UserJpaEntity();
        when(jpaRepository.findById(user.getId())).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(user);

        // Act & Assert
        assertThat(repository.findById(user.getId())).contains(user);
    }

    @Test
    void shouldReturnEmptyWhenIdIsUnknown() {
        // Arrange
        var id = UUID.randomUUID();
        when(jpaRepository.findById(id)).thenReturn(Optional.empty());

        // Act & Assert
        assertThat(repository.findById(id)).isEmpty();
    }
}
