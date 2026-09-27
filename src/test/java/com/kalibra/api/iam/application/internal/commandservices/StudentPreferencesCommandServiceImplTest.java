package com.kalibra.api.iam.application.internal.commandservices;

import com.kalibra.api.iam.domain.model.aggregates.StudentPreferences;
import com.kalibra.api.iam.domain.model.commands.UpdateDailyReminderCommand;
import com.kalibra.api.iam.domain.model.commands.UpdateDarkModeCommand;
import com.kalibra.api.iam.domain.repositories.StudentPreferencesRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentPreferencesCommandServiceImplTest {

    @Mock
    StudentPreferencesRepository studentPreferencesRepository;

    @InjectMocks
    StudentPreferencesCommandServiceImpl service;

    @Test
    void shouldCreatePreferencesWhenUpdatingDailyReminderOfAHolderWithoutThem() {
        // Arrange
        var command = new UpdateDailyReminderCommand("holder-123", true, LocalTime.of(19, 30));
        var captor = ArgumentCaptor.forClass(StudentPreferences.class);
        when(studentPreferencesRepository.findByHolderId("holder-123")).thenReturn(Optional.empty());
        when(studentPreferencesRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        var result = service.handle(command);

        // Assert
        verify(studentPreferencesRepository).save(captor.capture());
        assertThat(captor.getValue().getHolderId()).isEqualTo("holder-123");
        assertThat(result.getDailyReminder().enabled()).isTrue();
        assertThat(result.getDailyReminder().time()).isEqualTo(LocalTime.of(19, 30));
    }

    @Test
    void shouldUpdateDailyReminderOfExistingPreferences() {
        // Arrange
        var existing = StudentPreferences.createDefaultFor("holder-123");
        var command = new UpdateDailyReminderCommand("holder-123", true, LocalTime.of(7, 0));
        when(studentPreferencesRepository.findByHolderId("holder-123")).thenReturn(Optional.of(existing));
        when(studentPreferencesRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        var result = service.handle(command);

        // Assert
        assertThat(result.getId()).isEqualTo(existing.getId());
        assertThat(result.getDailyReminder().time()).isEqualTo(LocalTime.of(7, 0));
    }

    @Test
    void shouldUpdateDarkModeOfExistingPreferences() {
        // Arrange
        var existing = StudentPreferences.createDefaultFor("holder-123");
        when(studentPreferencesRepository.findByHolderId("holder-123")).thenReturn(Optional.of(existing));
        when(studentPreferencesRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        var result = service.handle(new UpdateDarkModeCommand("holder-123", true));

        // Assert
        assertThat(result.isDarkMode()).isTrue();
    }

    @Test
    void shouldCreatePreferencesWhenUpdatingDarkModeOfAHolderWithoutThem() {
        // Arrange
        when(studentPreferencesRepository.findByHolderId("holder-123")).thenReturn(Optional.empty());
        when(studentPreferencesRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        var result = service.handle(new UpdateDarkModeCommand("holder-123", true));

        // Assert
        assertThat(result.getHolderId()).isEqualTo("holder-123");
        assertThat(result.isDarkMode()).isTrue();
    }
}
