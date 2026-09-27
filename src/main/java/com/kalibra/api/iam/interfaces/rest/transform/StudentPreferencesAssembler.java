package com.kalibra.api.iam.interfaces.rest.transform;

import com.kalibra.api.iam.domain.model.aggregates.StudentPreferences;
import com.kalibra.api.iam.domain.model.commands.UpdateDailyReminderCommand;
import com.kalibra.api.iam.domain.model.commands.UpdateDarkModeCommand;
import com.kalibra.api.iam.interfaces.rest.resources.StudentPreferencesResource;
import com.kalibra.api.iam.interfaces.rest.resources.UpdateDailyReminderResource;
import com.kalibra.api.iam.interfaces.rest.resources.UpdateDarkModeResource;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface StudentPreferencesAssembler {

    @Mapping(target = "holderId", source = "holderId")
    @Mapping(target = "enabled", source = "resource.enabled")
    @Mapping(target = "time", source = "resource.time")
    UpdateDailyReminderCommand toCommand(UpdateDailyReminderResource resource, String holderId);

    @Mapping(target = "holderId", source = "holderId")
    @Mapping(target = "enabled", source = "resource.enabled")
    UpdateDarkModeCommand toCommand(UpdateDarkModeResource resource, String holderId);

    @Mapping(target = "dailyReminderEnabled", source = "preferences.dailyReminder.enabled")
    @Mapping(target = "dailyReminderTime", source = "preferences.dailyReminder.time")
    @Mapping(target = "darkMode", source = "preferences.darkMode")
    StudentPreferencesResource toResource(StudentPreferences preferences);
}
