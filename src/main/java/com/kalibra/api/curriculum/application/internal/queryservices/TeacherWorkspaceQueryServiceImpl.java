package com.kalibra.api.curriculum.application.internal.queryservices;

import com.kalibra.api.curriculum.domain.model.aggregates.TeacherWorkspace;
import com.kalibra.api.curriculum.domain.model.queries.GetTeacherWorkspaceByHolderIdQuery;
import com.kalibra.api.curriculum.domain.repositories.TeacherWorkspaceRepository;
import com.kalibra.api.curriculum.domain.services.TeacherWorkspaceQueryService;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class TeacherWorkspaceQueryServiceImpl implements TeacherWorkspaceQueryService {

    private final TeacherWorkspaceRepository teacherWorkspaceRepository;

    public TeacherWorkspaceQueryServiceImpl(TeacherWorkspaceRepository teacherWorkspaceRepository) {
        this.teacherWorkspaceRepository = teacherWorkspaceRepository;
    }

    @Override
    public Optional<TeacherWorkspace> handle(GetTeacherWorkspaceByHolderIdQuery query) {
        return teacherWorkspaceRepository.findByHolderId(query.holderId());
    }
}
