package com.kalibra.api.iam.infrastructure.persistence.repositories;

import com.kalibra.api.iam.domain.model.aggregates.StudentPreferences;
import com.kalibra.api.iam.domain.repositories.StudentPreferencesRepository;
import com.kalibra.api.iam.infrastructure.persistence.transform.StudentPreferencesJpaMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public class StudentPreferencesRepositoryImpl implements StudentPreferencesRepository {

    private final StudentPreferencesJpaRepository jpaRepository;
    private final StudentPreferencesJpaMapper mapper;

    public StudentPreferencesRepositoryImpl(StudentPreferencesJpaRepository jpaRepository,
                                             StudentPreferencesJpaMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public StudentPreferences save(StudentPreferences preferences) {
        var entity = mapper.toEntity(preferences);
        var saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<StudentPreferences> findByHolderId(String holderId) {
        return jpaRepository.findByHolderId(holderId).map(mapper::toDomain);
    }

    @Override
    public List<StudentPreferences> findAllByReminderTime(LocalTime time) {
        return jpaRepository.findAllByReminderTime(time).stream().map(mapper::toDomain).toList();
    }
}
