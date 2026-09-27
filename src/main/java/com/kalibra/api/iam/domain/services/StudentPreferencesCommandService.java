package com.kalibra.api.iam.domain.services;

import com.kalibra.api.iam.domain.model.aggregates.StudentPreferences;
import com.kalibra.api.iam.domain.model.commands.UpdateDailyReminderCommand;
import com.kalibra.api.iam.domain.model.commands.UpdateDarkModeCommand;

public interface StudentPreferencesCommandService {

    StudentPreferences handle(UpdateDailyReminderCommand command);

    StudentPreferences handle(UpdateDarkModeCommand command);
}
