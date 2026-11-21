package com.kalibra.api.curriculum.domain.repositories;

import com.kalibra.api.curriculum.domain.model.aggregates.TeacherWorkspace;

import java.util.Optional;

public interface TeacherWorkspaceRepository {

    TeacherWorkspace save(TeacherWorkspace workspace);

    Optional<TeacherWorkspace> findByHolderId(String holderId);
}
