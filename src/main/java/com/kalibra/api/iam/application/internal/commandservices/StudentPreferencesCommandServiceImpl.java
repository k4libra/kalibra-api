package com.kalibra.api.iam.application.internal.commandservices;

import com.kalibra.api.iam.domain.model.aggregates.StudentPreferences;
import com.kalibra.api.iam.domain.model.commands.UpdateDailyReminderCommand;
import com.kalibra.api.iam.domain.model.commands.UpdateDarkModeCommand;
import com.kalibra.api.iam.domain.repositories.StudentPreferencesRepository;
import com.kalibra.api.iam.domain.services.StudentPreferencesCommandService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudentPreferencesCommandServiceImpl implements StudentPreferencesCommandService {

    private final StudentPreferencesRepository studentPreferencesRepository;

    public StudentPreferencesCommandServiceImpl(StudentPreferencesRepository studentPreferencesRepository) {
        this.studentPreferencesRepository = studentPreferencesRepository;
    }

    @Override
    @Transactional
    public StudentPreferences handle(UpdateDailyReminderCommand command) {
        var preferences = studentPreferencesRepository.findByHolderId(command.holderId())
                .orElseGet(() -> StudentPreferences.createDefaultFor(command.holderId()));
        preferences.update(command);
        return studentPreferencesRepository.save(preferences);
    }

    @Override
    @Transactional
    public StudentPreferences handle(UpdateDarkModeCommand command) {
        var preferences = studentPreferencesRepository.findByHolderId(command.holderId())
                .orElseGet(() -> StudentPreferences.createDefaultFor(command.holderId()));
        preferences.update(command);
        return studentPreferencesRepository.save(preferences);
    }
}
