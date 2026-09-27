package com.kalibra.api.curriculum.infrastructure.persistence.repositories;

import com.kalibra.api.curriculum.domain.model.aggregates.TeacherWorkspace;
import com.kalibra.api.curriculum.domain.repositories.TeacherWorkspaceRepository;
import com.kalibra.api.curriculum.infrastructure.persistence.transform.TeacherWorkspaceJpaMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class TeacherWorkspaceRepositoryImpl implements TeacherWorkspaceRepository {

    private final TeacherWorkspaceJpaRepository jpaRepository;
    private final TeacherWorkspaceJpaMapper mapper;

    public TeacherWorkspaceRepositoryImpl(TeacherWorkspaceJpaRepository jpaRepository, TeacherWorkspaceJpaMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public TeacherWorkspace save(TeacherWorkspace workspace) {
        var entity = mapper.toEntity(workspace);
        var saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<TeacherWorkspace> findByHolderId(String holderId) {
        return jpaRepository.findByHolderId(holderId).map(mapper::toDomain);
    }
}
