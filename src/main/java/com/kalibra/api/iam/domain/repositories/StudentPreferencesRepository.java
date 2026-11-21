package com.kalibra.api.iam.domain.repositories;

import com.kalibra.api.iam.domain.model.aggregates.StudentPreferences;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface StudentPreferencesRepository {

    StudentPreferences save(StudentPreferences preferences);

    Optional<StudentPreferences> findByHolderId(String holderId);

    List<StudentPreferences> findAllByReminderTime(LocalTime time);
}
