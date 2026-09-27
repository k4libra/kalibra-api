package com.kalibra.api.iam.domain.model.aggregates;

import com.kalibra.api.iam.domain.model.commands.UpdateDailyReminderCommand;
import com.kalibra.api.iam.domain.model.commands.UpdateDarkModeCommand;
import com.kalibra.api.iam.domain.model.valueobjects.DailyReminder;

import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public class StudentPreferences {

    private UUID id;
    private String holderId;
    private DailyReminder dailyReminder;
    private boolean darkMode;

    // public: required by MapStruct, which generates its mapper impl in a different package.
    public StudentPreferences() {
    }

    private StudentPreferences(String holderId) {
        this.id = UUID.randomUUID();
        this.holderId = holderId;
        this.dailyReminder = DailyReminder.disabled();
        this.darkMode = false;
    }

    public static StudentPreferences createDefaultFor(String holderId) {
        return new StudentPreferences(holderId);
    }

    public void update(UpdateDailyReminderCommand command) {
        this.dailyReminder = new DailyReminder(command.enabled(), command.time());
    }

    public void update(UpdateDarkModeCommand command) {
        this.darkMode = command.enabled();
    }

    public boolean isReminderDue(LocalTime now) {
        return dailyReminder.enabled() && dailyReminder.time().equals(now.truncatedTo(ChronoUnit.MINUTES));
    }

    public UUID getId() {
        return id;
    }

    public String getHolderId() {
        return holderId;
    }

    public DailyReminder getDailyReminder() {
        return dailyReminder;
    }

    public boolean isDarkMode() {
        return darkMode;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public void setHolderId(String holderId) {
        this.holderId = holderId;
    }

    public void setDailyReminder(DailyReminder dailyReminder) {
        this.dailyReminder = dailyReminder;
    }

    public void setDarkMode(boolean darkMode) {
        this.darkMode = darkMode;
    }
}
