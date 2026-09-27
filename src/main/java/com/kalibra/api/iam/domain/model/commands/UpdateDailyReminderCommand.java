package com.kalibra.api.iam.domain.model.commands;

import java.time.LocalTime;

public record UpdateDailyReminderCommand(String holderId, boolean enabled, LocalTime time) { }
