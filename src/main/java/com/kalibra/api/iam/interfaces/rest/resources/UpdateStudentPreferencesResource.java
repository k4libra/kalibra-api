package com.kalibra.api.iam.interfaces.rest.resources;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalTime;

public record UpdateStudentPreferencesResource(Boolean dailyReminderEnabled,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm[:ss]")
        @Schema(type = "string", example = "19:30", description = "Reminder time in UTC (HH:mm or HH:mm:ss)")
        LocalTime dailyReminderTime, Boolean darkMode) { }
