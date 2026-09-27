package com.kalibra.api.curriculum.domain.model.aggregates;

import com.kalibra.api.curriculum.domain.model.commands.SelectActiveCourseCommand;
import com.kalibra.api.curriculum.domain.model.valueobjects.CourseId;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TeacherWorkspaceTest {

    @Test
    void shouldStartWithoutActiveCourse() {
        var workspace = TeacherWorkspace.createFor("teacher-1");

        assertThat(workspace.getId()).isNotNull();
        assertThat(workspace.getHolderId()).isEqualTo("teacher-1");
        assertThat(workspace.getActiveCourseId()).isEmpty();
    }

    @Test
    void shouldActivateACourseAndReportWhetherItChanged() {
        // Arrange
        var workspace = TeacherWorkspace.createFor("teacher-1");
        var courseId = new CourseId(UUID.randomUUID());
        var command = new SelectActiveCourseCommand("teacher-1", courseId);

        // Act & Assert
        assertThat(workspace.activate(command)).isTrue();
        assertThat(workspace.getActiveCourseId()).contains(courseId);
        assertThat(workspace.activate(command)).isFalse();
    }

    @Test
    void shouldReplaceTheActiveCourse() {
        // Arrange
        var workspace = TeacherWorkspace.createFor("teacher-1");
        var other = new CourseId(UUID.randomUUID());
        workspace.activate(new SelectActiveCourseCommand("teacher-1", new CourseId(UUID.randomUUID())));

        // Act
        var changed = workspace.activate(new SelectActiveCourseCommand("teacher-1", other));

        // Assert
        assertThat(changed).isTrue();
        assertThat(workspace.getActiveCourseId()).contains(other);
    }
}
