package com.kalibra.api.curriculum.application.internal.commandservices;

import com.kalibra.api.curriculum.domain.exceptions.CourseNotOwnedByTeacherException;
import com.kalibra.api.curriculum.domain.model.aggregates.Course;
import com.kalibra.api.curriculum.domain.model.aggregates.TeacherWorkspace;
import com.kalibra.api.curriculum.domain.model.commands.CreateCourseCommand;
import com.kalibra.api.curriculum.domain.model.commands.SelectActiveCourseCommand;
import com.kalibra.api.curriculum.domain.model.valueobjects.CourseCode;
import com.kalibra.api.curriculum.domain.repositories.CourseRepository;
import com.kalibra.api.curriculum.domain.repositories.TeacherWorkspaceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeacherWorkspaceCommandServiceImplTest {

    @Mock
    TeacherWorkspaceRepository teacherWorkspaceRepository;

    @Mock
    CourseRepository courseRepository;

    @InjectMocks
    TeacherWorkspaceCommandServiceImpl service;

    private final Course course = Course.create(
            new CreateCourseCommand("teacher-1", "Algebra", new CourseCode("MAT101"), List.of("Equations")));

    @Test
    void shouldCreateWorkspaceWithTheActiveCourseOnFirstUse() {
        // Arrange
        var command = new SelectActiveCourseCommand("teacher-1", course.getId());
        when(courseRepository.findByIdAndHolderId(course.getId(), "teacher-1")).thenReturn(Optional.of(course));
        when(teacherWorkspaceRepository.findByHolderId("teacher-1")).thenReturn(Optional.empty());
        when(teacherWorkspaceRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        var workspace = service.handle(command);

        // Assert
        assertThat(workspace.getHolderId()).isEqualTo("teacher-1");
        assertThat(workspace.getActiveCourseId()).contains(course.getId());
    }

    @Test
    void shouldNotSaveWhenTheCourseIsAlreadyActive() {
        // Arrange
        var command = new SelectActiveCourseCommand("teacher-1", course.getId());
        var workspace = TeacherWorkspace.createFor("teacher-1");
        workspace.activate(command);
        when(courseRepository.findByIdAndHolderId(course.getId(), "teacher-1")).thenReturn(Optional.of(course));
        when(teacherWorkspaceRepository.findByHolderId("teacher-1")).thenReturn(Optional.of(workspace));

        // Act
        var result = service.handle(command);

        // Assert
        assertThat(result.getActiveCourseId()).contains(course.getId());
        verify(teacherWorkspaceRepository, never()).save(any());
    }

    @Test
    void shouldActivateOnTheWorkspaceCreatedByAConcurrentFirstSelection() {
        // Arrange
        var command = new SelectActiveCourseCommand("teacher-1", course.getId());
        var concurrentlyCreated = TeacherWorkspace.createFor("teacher-1");
        when(courseRepository.findByIdAndHolderId(course.getId(), "teacher-1")).thenReturn(Optional.of(course));
        when(teacherWorkspaceRepository.findByHolderId("teacher-1"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(concurrentlyCreated));
        when(teacherWorkspaceRepository.save(any()))
                .thenThrow(new DataIntegrityViolationException("teacher_workspaces_holder_id_key"))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        var workspace = service.handle(command);

        // Assert
        assertThat(workspace.getId()).isEqualTo(concurrentlyCreated.getId());
        assertThat(workspace.getActiveCourseId()).contains(course.getId());
    }

    @Test
    void shouldRejectACourseOfAnotherTeacher() {
        // Arrange
        var command = new SelectActiveCourseCommand("teacher-2", course.getId());
        when(courseRepository.findByIdAndHolderId(course.getId(), "teacher-2")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> service.handle(command)).isInstanceOf(CourseNotOwnedByTeacherException.class);
        verify(teacherWorkspaceRepository, never()).save(any());
    }
}
