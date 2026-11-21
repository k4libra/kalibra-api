package com.kalibra.api.curriculum.infrastructure.persistence.repositories;

import com.kalibra.api.curriculum.infrastructure.persistence.entities.CourseJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CourseJpaRepository extends JpaRepository<CourseJpaEntity, UUID> {

    Optional<CourseJpaEntity> findByIdAndHolderId(UUID id, String holderId);

    List<CourseJpaEntity> findAllByHolderId(String holderId);
}
