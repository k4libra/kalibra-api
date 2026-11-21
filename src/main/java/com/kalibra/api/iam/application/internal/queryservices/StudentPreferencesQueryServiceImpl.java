package com.kalibra.api.iam.application.internal.queryservices;

import com.kalibra.api.iam.domain.model.aggregates.StudentPreferences;
import com.kalibra.api.iam.domain.model.queries.GetStudentPreferencesByHolderIdQuery;
import com.kalibra.api.iam.domain.model.queries.GetStudentPreferencesWithReminderDueQuery;
import com.kalibra.api.iam.domain.repositories.StudentPreferencesRepository;
import com.kalibra.api.iam.domain.services.StudentPreferencesQueryService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class StudentPreferencesQueryServiceImpl implements StudentPreferencesQueryService {

    private final StudentPreferencesRepository studentPreferencesRepository;

    public StudentPreferencesQueryServiceImpl(StudentPreferencesRepository studentPreferencesRepository) {
        this.studentPreferencesRepository = studentPreferencesRepository;
    }

    @Override
    public Optional<StudentPreferences> handle(GetStudentPreferencesByHolderIdQuery query) {
        return studentPreferencesRepository.findByHolderId(query.holderId());
    }

    @Override
    public List<StudentPreferences> handle(GetStudentPreferencesWithReminderDueQuery query) {
        return studentPreferencesRepository.findAllByReminderTime(query.time()).stream()
                .filter(preferences -> preferences.isReminderDue(query.time()))
                .toList();
    }
}
