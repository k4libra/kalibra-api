package com.kalibra.api.iam.domain.model.aggregates;

import com.kalibra.api.iam.domain.model.commands.UpdateStudentPreferencesCommand;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
        preferences.update(new UpdateStudentPreferencesCommand("holder-123", Optional.of(true), Optional.of(LocalTime.of(19, 30)), Optional.empty()));

        // Assert
        assertThat(preferences.getDailyReminder().enabled()).isTrue();
        assertThat(preferences.getDailyReminder().time()).isEqualTo(LocalTime.of(19, 30));
    }

    @Test
    void shouldUpdateDarkMode() {
        // Arrange
        var preferences = StudentPreferences.createDefaultFor("holder-123");

        // Act
        preferences.update(new UpdateStudentPreferencesCommand("holder-123", Optional.empty(), Optional.empty(), Optional.of(true)));

        // Assert
        assertThat(preferences.isDarkMode()).isTrue();
    }

    @Test
    void shouldBeDueOnlyAtTheReminderMinuteWhenEnabled() {
        // Arrange
        var preferences = StudentPreferences.createDefaultFor("holder-123");
        preferences.update(new UpdateStudentPreferencesCommand("holder-123", Optional.of(true), Optional.of(LocalTime.of(19, 30)), Optional.empty()));

        // Act & Assert
        assertThat(preferences.isReminderDue(LocalTime.of(19, 30, 45))).isTrue();
        assertThat(preferences.isReminderDue(LocalTime.of(19, 31))).isFalse();
    }

    @Test
    void shouldNeverBeDueWhenReminderIsDisabled() {
        // Arrange
        var preferences = StudentPreferences.createDefaultFor("holder-123");
        preferences.update(new UpdateStudentPreferencesCommand("holder-123", Optional.of(false), Optional.of(LocalTime.of(19, 30)), Optional.empty()));

        // Act & Assert
        assertThat(preferences.isReminderDue(LocalTime.of(19, 30))).isFalse();
    }

    @Test
    void shouldRejectEnablingWithoutTimeBeforeChangingDarkMode() {
        var preferences = StudentPreferences.createDefaultFor("holder-123");
        assertThatThrownBy(() -> preferences.update(new UpdateStudentPreferencesCommand("holder-123",
                Optional.of(true), Optional.empty(), Optional.of(true))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(preferences.isDarkMode()).isFalse();
        assertThat(preferences.getDailyReminder().enabled()).isFalse();
    }

    @Test
    void shouldEnableWithStoredTimeAndPreserveOmittedPreferences() {
        var preferences = StudentPreferences.createDefaultFor("holder-123");
        preferences.update(new UpdateStudentPreferencesCommand("holder-123", Optional.empty(),
                Optional.of(LocalTime.of(7, 15, 30)), Optional.of(true)));
        preferences.update(new UpdateStudentPreferencesCommand("holder-123", Optional.of(true), Optional.empty(), Optional.empty()));
        assertThat(preferences.getDailyReminder().enabled()).isTrue();
        assertThat(preferences.getDailyReminder().time()).isEqualTo(LocalTime.of(7, 15));
        assertThat(preferences.isDarkMode()).isTrue();
        preferences.update(new UpdateStudentPreferencesCommand("holder-123", Optional.empty(), Optional.empty(), Optional.of(false)));
        assertThat(preferences.isDarkMode()).isFalse();
        assertThat(preferences.getDailyReminder().enabled()).isTrue();
    }

    @Test
    void shouldPreserveTimeWhenDisablingAndTreatEmptyPatchAsNoOp() {
        var preferences = StudentPreferences.createDefaultFor("holder-123");
        preferences.update(new UpdateStudentPreferencesCommand("holder-123", Optional.of(true), Optional.of(LocalTime.of(19, 30)), Optional.of(true)));
        preferences.update(new UpdateStudentPreferencesCommand("holder-123", Optional.of(false), Optional.empty(), Optional.empty()));
        preferences.update(new UpdateStudentPreferencesCommand("holder-123", Optional.empty(), Optional.empty(), Optional.empty()));
        assertThat(preferences.getDailyReminder().enabled()).isFalse();
        assertThat(preferences.getDailyReminder().time()).isEqualTo(LocalTime.of(19, 30));
        assertThat(preferences.isDarkMode()).isTrue();
    }
}
