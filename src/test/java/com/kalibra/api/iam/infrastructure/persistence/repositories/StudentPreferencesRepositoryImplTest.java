package com.kalibra.api.iam.infrastructure.persistence.repositories;

import com.kalibra.api.iam.domain.model.aggregates.StudentPreferences;
import com.kalibra.api.iam.infrastructure.persistence.entities.StudentPreferencesJpaEntity;
import com.kalibra.api.iam.infrastructure.persistence.transform.StudentPreferencesJpaMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentPreferencesRepositoryImplTest {

    @Mock
    StudentPreferencesJpaRepository jpaRepository;

    @Mock
    StudentPreferencesJpaMapper mapper;

    @InjectMocks
    StudentPreferencesRepositoryImpl repository;

    @Test
    void shouldMapSaveAndMapBackWhenSaving() {
        // Arrange
        var preferences = StudentPreferences.createDefaultFor("holder-123");
        var entity = new StudentPreferencesJpaEntity();
        when(mapper.toEntity(preferences)).thenReturn(entity);
        when(jpaRepository.save(entity)).thenReturn(entity);
        when(mapper.toDomain(entity)).thenReturn(preferences);

        // Act
        var result = repository.save(preferences);

        // Assert
        verify(jpaRepository).save(entity);
        assertThat(result).isEqualTo(preferences);
    }

    @Test
    void shouldReturnPreferencesWhenHolderHasThem() {
        // Arrange
        var entity = new StudentPreferencesJpaEntity();
        var preferences = StudentPreferences.createDefaultFor("holder-123");
        when(jpaRepository.findByHolderId("holder-123")).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(preferences);

        // Act & Assert
        assertThat(repository.findByHolderId("holder-123")).contains(preferences);
    }

    @Test
    void shouldReturnEmptyWhenHolderHasNoPreferences() {
        // Arrange
        when(jpaRepository.findByHolderId("holder-123")).thenReturn(Optional.empty());

        // Act & Assert
        assertThat(repository.findByHolderId("holder-123")).isEmpty();
    }

    @Test
    void shouldMapEveryEntityWhenFindingByReminderTime() {
        // Arrange
        var entity = new StudentPreferencesJpaEntity();
        var preferences = StudentPreferences.createDefaultFor("holder-123");
        when(jpaRepository.findAllByReminderTime(LocalTime.of(19, 30))).thenReturn(List.of(entity));
        when(mapper.toDomain(entity)).thenReturn(preferences);

        // Act & Assert
        assertThat(repository.findAllByReminderTime(LocalTime.of(19, 30))).containsExactly(preferences);
    }
}
