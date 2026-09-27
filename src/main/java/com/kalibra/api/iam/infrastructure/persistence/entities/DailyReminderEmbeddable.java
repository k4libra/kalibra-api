package com.kalibra.api.iam.infrastructure.persistence.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.time.LocalTime;

@Embeddable
public class DailyReminderEmbeddable {

    @Column(name = "daily_reminder_enabled", nullable = false)
    private boolean enabled;

    @Column(name = "daily_reminder_time")
    private LocalTime reminderTime;

    // public: required by MapStruct, which generates its mapper impl in a different package.
    public DailyReminderEmbeddable() {
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public LocalTime getReminderTime() {
        return reminderTime;
    }

    public void setReminderTime(LocalTime reminderTime) {
        this.reminderTime = reminderTime;
    }
}
