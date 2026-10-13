package com.kalibra.api.iam.interfaces.rest.transform;

import com.kalibra.api.iam.domain.model.aggregates.StudentPreferences;
import com.kalibra.api.iam.domain.model.commands.UpdateStudentPreferencesCommand;
import com.kalibra.api.iam.interfaces.rest.resources.StudentPreferencesResource;
import com.kalibra.api.iam.interfaces.rest.resources.UpdateStudentPreferencesResource;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalTime;
import java.util.Optional;

@Mapper(componentModel = "spring")
public interface StudentPreferencesAssembler {

    UpdateStudentPreferencesCommand toCommand(UpdateStudentPreferencesResource resource, String holderId);

    default Optional<Boolean> toOptionalBoolean(Boolean value) {
        return Optional.ofNullable(value);
    }

    default Optional<LocalTime> toOptionalTime(LocalTime value) {
        return Optional.ofNullable(value);
    }

    @Mapping(target = "dailyReminderEnabled", source = "preferences.dailyReminder.enabled")
    @Mapping(target = "dailyReminderTime", source = "preferences.dailyReminder.time")
    @Mapping(target = "darkMode", source = "preferences.darkMode")
    StudentPreferencesResource toResource(StudentPreferences preferences);
}
