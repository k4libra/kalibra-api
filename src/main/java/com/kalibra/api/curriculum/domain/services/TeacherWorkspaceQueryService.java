package com.kalibra.api.curriculum.domain.services;

import com.kalibra.api.curriculum.domain.model.aggregates.TeacherWorkspace;
import com.kalibra.api.curriculum.domain.model.queries.GetTeacherWorkspaceByHolderIdQuery;

import java.util.Optional;

public interface TeacherWorkspaceQueryService {

    Optional<TeacherWorkspace> handle(GetTeacherWorkspaceByHolderIdQuery query);
}
