package com.kalibra.api.iam.interfaces.rest.resources;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;

import java.time.LocalTime;

public record UpdateDailyReminderResource(
        boolean enabled,
        @Schema(type = "string", example = "19:30", description = "Reminder time in UTC (HH:mm); required when enabled")
        LocalTime time
) {

    @JsonIgnore
    @AssertTrue(message = "time is required when the reminder is enabled")
    public boolean isTimeProvidedWhenEnabled() {
        return !enabled || time != null;
    }
}
