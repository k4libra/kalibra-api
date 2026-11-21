package com.kalibra.api.iam.domain.services;

import com.kalibra.api.iam.domain.model.aggregates.StudentPreferences;
import com.kalibra.api.iam.domain.model.queries.GetStudentPreferencesByHolderIdQuery;
import com.kalibra.api.iam.domain.model.queries.GetStudentPreferencesWithReminderDueQuery;

import java.util.List;
import java.util.Optional;

public interface StudentPreferencesQueryService {

    Optional<StudentPreferences> handle(GetStudentPreferencesByHolderIdQuery query);

    List<StudentPreferences> handle(GetStudentPreferencesWithReminderDueQuery query);
}
