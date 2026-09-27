package com.kalibra.api.iam.infrastructure.persistence.transform;

import com.kalibra.api.iam.domain.model.aggregates.StudentPreferences;
import com.kalibra.api.iam.domain.model.valueobjects.DailyReminder;
import com.kalibra.api.iam.infrastructure.persistence.entities.DailyReminderEmbeddable;
import com.kalibra.api.iam.infrastructure.persistence.entities.StudentPreferencesJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface StudentPreferencesJpaMapper {

    StudentPreferencesJpaEntity toEntity(StudentPreferences studentPreferences);

    StudentPreferences toDomain(StudentPreferencesJpaEntity entity);

    @Mapping(target = "reminderTime", source = "time")
    DailyReminderEmbeddable toEmbeddable(DailyReminder dailyReminder);

    @Mapping(target = "time", source = "reminderTime")
    DailyReminder toDailyReminder(DailyReminderEmbeddable embeddable);
}
