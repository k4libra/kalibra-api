package com.kalibra.api.curriculum.infrastructure.persistence.repositories;

import com.kalibra.api.curriculum.infrastructure.persistence.entities.TeacherWorkspaceJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TeacherWorkspaceJpaRepository extends JpaRepository<TeacherWorkspaceJpaEntity, UUID> {

    Optional<TeacherWorkspaceJpaEntity> findByHolderId(String holderId);
}
