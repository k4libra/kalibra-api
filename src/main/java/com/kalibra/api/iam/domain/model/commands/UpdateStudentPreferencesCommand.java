package com.kalibra.api.iam.domain.model.commands;

import java.time.LocalTime;
import java.util.Optional;

public record UpdateStudentPreferencesCommand(String holderId, Optional<Boolean> dailyReminderEnabled,
                                             Optional<LocalTime> dailyReminderTime, Optional<Boolean> darkMode) { }
