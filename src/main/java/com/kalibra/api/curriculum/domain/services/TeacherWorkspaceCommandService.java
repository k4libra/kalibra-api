package com.kalibra.api.curriculum.domain.services;

import com.kalibra.api.curriculum.domain.model.aggregates.TeacherWorkspace;
import com.kalibra.api.curriculum.domain.model.commands.SelectActiveCourseCommand;

public interface TeacherWorkspaceCommandService {

    TeacherWorkspace handle(SelectActiveCourseCommand command);
}
