package com.kalibra.api.iam.domain.model.valueobjects;

import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DailyReminderTest {

    @Test
    void shouldRejectEnabledReminderWithoutTime() {
        assertThatThrownBy(() -> new DailyReminder(true, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldAllowDisabledReminderWithoutTime() {
        assertThat(new DailyReminder(false, null).time()).isNull();
    }

    @Test
    void shouldTruncateTimeToMinutes() {
        assertThat(new DailyReminder(true, LocalTime.of(8, 15, 42)).time()).isEqualTo(LocalTime.of(8, 15));
    }
}
