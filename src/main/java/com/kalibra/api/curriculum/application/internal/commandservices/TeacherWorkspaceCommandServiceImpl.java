package com.kalibra.api.curriculum.application.internal.commandservices;

import com.kalibra.api.curriculum.domain.exceptions.CourseNotOwnedByTeacherException;
import com.kalibra.api.curriculum.domain.model.aggregates.TeacherWorkspace;
import com.kalibra.api.curriculum.domain.model.commands.SelectActiveCourseCommand;
import com.kalibra.api.curriculum.domain.repositories.CourseRepository;
import com.kalibra.api.curriculum.domain.repositories.TeacherWorkspaceRepository;
import com.kalibra.api.curriculum.domain.services.TeacherWorkspaceCommandService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class TeacherWorkspaceCommandServiceImpl implements TeacherWorkspaceCommandService {

    private final TeacherWorkspaceRepository teacherWorkspaceRepository;
    private final CourseRepository courseRepository;

    public TeacherWorkspaceCommandServiceImpl(TeacherWorkspaceRepository teacherWorkspaceRepository,
                                               CourseRepository courseRepository) {
        this.teacherWorkspaceRepository = teacherWorkspaceRepository;
        this.courseRepository = courseRepository;
    }

    @Override
    public TeacherWorkspace handle(SelectActiveCourseCommand command) {
        courseRepository.findByIdAndHolderId(command.courseId(), command.holderId())
                .orElseThrow(() -> new CourseNotOwnedByTeacherException(command.courseId().value()));
        try {
            return activateAndSave(command);
        } catch (DataIntegrityViolationException concurrentFirstSelection) {
            return activateAndSave(command);
        }
    }

    private TeacherWorkspace activateAndSave(SelectActiveCourseCommand command) {
        var workspace = teacherWorkspaceRepository.findByHolderId(command.holderId())
                .orElseGet(() -> TeacherWorkspace.createFor(command.holderId()));
        if (!workspace.activate(command)) {
            return workspace;
        }
        return teacherWorkspaceRepository.save(workspace);
    }
}
