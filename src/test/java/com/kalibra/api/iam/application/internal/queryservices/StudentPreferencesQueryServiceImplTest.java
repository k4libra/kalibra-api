package com.kalibra.api.iam.application.internal.queryservices;

import com.kalibra.api.iam.domain.model.aggregates.StudentPreferences;
import com.kalibra.api.iam.domain.model.commands.UpdateDailyReminderCommand;
import com.kalibra.api.iam.domain.model.queries.GetStudentPreferencesByHolderIdQuery;
import com.kalibra.api.iam.domain.model.queries.GetStudentPreferencesWithReminderDueQuery;
import com.kalibra.api.iam.domain.repositories.StudentPreferencesRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentPreferencesQueryServiceImplTest {

    @Mock
    StudentPreferencesRepository studentPreferencesRepository;

    @InjectMocks
    StudentPreferencesQueryServiceImpl service;

    @Test
    void shouldReturnPreferencesWhenHolderHasThem() {
        // Arrange
        var preferences = StudentPreferences.createDefaultFor("holder-123");
        when(studentPreferencesRepository.findByHolderId("holder-123")).thenReturn(Optional.of(preferences));

        // Act & Assert
        assertThat(service.handle(new GetStudentPreferencesByHolderIdQuery("holder-123"))).contains(preferences);
    }

    @Test
    void shouldReturnEmptyWhenHolderHasNoPreferences() {
        // Arrange
        when(studentPreferencesRepository.findByHolderId("holder-123")).thenReturn(Optional.empty());

        // Act & Assert
        assertThat(service.handle(new GetStudentPreferencesByHolderIdQuery("holder-123"))).isEmpty();
    }

    @Test
    void shouldReturnOnlyPreferencesWhoseReminderIsDue() {
        // Arrange
        var due = StudentPreferences.createDefaultFor("holder-due");
        due.update(new UpdateDailyReminderCommand("holder-due", true, LocalTime.of(19, 30)));
        var notDue = StudentPreferences.createDefaultFor("holder-not-due");
        when(studentPreferencesRepository.findAllByReminderTime(LocalTime.of(19, 30)))
                .thenReturn(List.of(due, notDue));

        // Act
        var result = service.handle(new GetStudentPreferencesWithReminderDueQuery(LocalTime.of(19, 30)));

        // Assert
        assertThat(result).containsExactly(due);
    }

    @Test
    void shouldReturnEmptyListWhenNoReminderIsDue() {
        // Arrange
        when(studentPreferencesRepository.findAllByReminderTime(LocalTime.of(3, 0))).thenReturn(List.of());

        // Act & Assert
        assertThat(service.handle(new GetStudentPreferencesWithReminderDueQuery(LocalTime.of(3, 0)))).isEmpty();
    }
}
