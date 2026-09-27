package com.kalibra.api.iam.domain.model.aggregates;

import com.kalibra.api.iam.domain.model.commands.UpdateDailyReminderCommand;
import com.kalibra.api.iam.domain.model.commands.UpdateDarkModeCommand;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

class StudentPreferencesTest {

    @Test
    void shouldCreateDefaultPreferencesForHolder() {
        // Act
        var preferences = StudentPreferences.createDefaultFor("holder-123");

        // Assert
        assertThat(preferences.getId()).isNotNull();
        assertThat(preferences.getHolderId()).isEqualTo("holder-123");
        assertThat(preferences.getDailyReminder().enabled()).isFalse();
        assertThat(preferences.getDailyReminder().time()).isNull();
        assertThat(preferences.isDarkMode()).isFalse();
    }

    @Test
    void shouldUpdateDailyReminder() {
        // Arrange
        var preferences = StudentPreferences.createDefaultFor("holder-123");

        // Act
        preferences.update(new UpdateDailyReminderCommand("holder-123", true, LocalTime.of(19, 30)));

        // Assert
        assertThat(preferences.getDailyReminder().enabled()).isTrue();
        assertThat(preferences.getDailyReminder().time()).isEqualTo(LocalTime.of(19, 30));
    }

    @Test
    void shouldUpdateDarkMode() {
        // Arrange
        var preferences = StudentPreferences.createDefaultFor("holder-123");

        // Act
        preferences.update(new UpdateDarkModeCommand("holder-123", true));

        // Assert
        assertThat(preferences.isDarkMode()).isTrue();
    }

    @Test
    void shouldBeDueOnlyAtTheReminderMinuteWhenEnabled() {
        // Arrange
        var preferences = StudentPreferences.createDefaultFor("holder-123");
        preferences.update(new UpdateDailyReminderCommand("holder-123", true, LocalTime.of(19, 30)));

        // Act & Assert
        assertThat(preferences.isReminderDue(LocalTime.of(19, 30, 45))).isTrue();
        assertThat(preferences.isReminderDue(LocalTime.of(19, 31))).isFalse();
    }

    @Test
    void shouldNeverBeDueWhenReminderIsDisabled() {
        // Arrange
        var preferences = StudentPreferences.createDefaultFor("holder-123");
        preferences.update(new UpdateDailyReminderCommand("holder-123", false, LocalTime.of(19, 30)));

        // Act & Assert
        assertThat(preferences.isReminderDue(LocalTime.of(19, 30))).isFalse();
    }
}
