package com.kalibra.api.iam.domain.model.valueobjects;

import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

public record DailyReminder(boolean enabled, LocalTime time) {

    public DailyReminder {
        time = time == null ? null : time.truncatedTo(ChronoUnit.MINUTES);
    }

    public static DailyReminder disabled() {
        return new DailyReminder(false, null);
    }
}
