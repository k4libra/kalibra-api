package com.kalibra.api.iam.domain.model.valueobjects;

import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

public record DailyReminder(boolean enabled, LocalTime time) {

    public DailyReminder {
        if (enabled && time == null) {
            throw new IllegalArgumentException("A daily reminder needs a time when it is enabled");
        }
        time = time == null ? null : time.truncatedTo(ChronoUnit.MINUTES);
    }

    public static DailyReminder disabled() {
        return new DailyReminder(false, null);
    }
}
