package com.kalibra.api.iam.infrastructure.persistence.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "student_preferences", schema = "iam")
public class StudentPreferencesJpaEntity {

    @Id
    private UUID id;

    @Column(name = "holder_id", nullable = false, unique = true, length = 64)
    private String holderId;

    @Embedded
    private DailyReminderEmbeddable dailyReminder;

    @Column(name = "dark_mode", nullable = false)
    private boolean darkMode;

    // public: required by MapStruct, which generates its mapper impl in a different package.
    public StudentPreferencesJpaEntity() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getHolderId() {
        return holderId;
    }

    public void setHolderId(String holderId) {
        this.holderId = holderId;
    }

    public DailyReminderEmbeddable getDailyReminder() {
        return dailyReminder;
    }

    public void setDailyReminder(DailyReminderEmbeddable dailyReminder) {
        this.dailyReminder = dailyReminder;
    }

    public boolean isDarkMode() {
        return darkMode;
    }

    public void setDarkMode(boolean darkMode) {
        this.darkMode = darkMode;
    }
}
