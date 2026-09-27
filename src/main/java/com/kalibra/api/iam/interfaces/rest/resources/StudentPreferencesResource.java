package com.kalibra.api.iam.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalTime;

public record StudentPreferencesResource(
        boolean dailyReminderEnabled,
        @Schema(type = "string", example = "19:30:00", description = "Reminder time in UTC; null when never set")
        LocalTime dailyReminderTime,
        boolean darkMode
) { }
