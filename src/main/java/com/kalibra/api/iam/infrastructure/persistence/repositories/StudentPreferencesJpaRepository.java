package com.kalibra.api.iam.infrastructure.persistence.repositories;

import com.kalibra.api.iam.infrastructure.persistence.entities.StudentPreferencesJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StudentPreferencesJpaRepository extends JpaRepository<StudentPreferencesJpaEntity, UUID> {

    Optional<StudentPreferencesJpaEntity> findByHolderId(String holderId);

    @Query("select p from StudentPreferencesJpaEntity p "
            + "where p.dailyReminder.enabled = true and p.dailyReminder.reminderTime = :time")
    List<StudentPreferencesJpaEntity> findAllByReminderTime(@Param("time") LocalTime time);
}
