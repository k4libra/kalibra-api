package com.kalibra.api.iam.domain.services;

import com.kalibra.api.iam.domain.model.aggregates.StudentPreferences;
import com.kalibra.api.iam.domain.model.commands.UpdateStudentPreferencesCommand;

public interface StudentPreferencesCommandService {

    StudentPreferences handle(UpdateStudentPreferencesCommand command);
}
