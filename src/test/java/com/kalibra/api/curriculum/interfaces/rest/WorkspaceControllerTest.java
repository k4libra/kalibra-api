package com.kalibra.api.curriculum.interfaces.rest;

import com.kalibra.api.curriculum.domain.exceptions.CourseNotOwnedByTeacherException;
import com.kalibra.api.curriculum.domain.model.aggregates.TeacherWorkspace;
import com.kalibra.api.curriculum.domain.model.commands.SelectActiveCourseCommand;
import com.kalibra.api.curriculum.domain.model.queries.GetTeacherWorkspaceByHolderIdQuery;
import com.kalibra.api.curriculum.domain.model.valueobjects.CourseId;
import com.kalibra.api.curriculum.domain.services.TeacherWorkspaceCommandService;
import com.kalibra.api.curriculum.domain.services.TeacherWorkspaceQueryService;
import com.kalibra.api.curriculum.interfaces.rest.transform.TeacherWorkspaceAssemblerImpl;
import com.kalibra.api.shared.config.JwtAuthenticationFilter;
import com.kalibra.api.shared.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WorkspaceController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, TeacherWorkspaceAssemblerImpl.class})
@TestPropertySource(properties = "kalibra.jwt.secret=test-secret-only-for-wiring-not-a-real-value")
class WorkspaceControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    TeacherWorkspaceCommandService commandService;

    @MockitoBean
    TeacherWorkspaceQueryService queryService;

    @Test
    void shouldAnswerNullActiveCourseWithoutCreatingAWorkspace() throws Exception {
        when(queryService.handle(new GetTeacherWorkspaceByHolderIdQuery("teacher-1"))).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/teachers/me/workspace").with(user("teacher-1").roles("TEACHER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeCourseId").doesNotExist());
        verify(commandService, never()).handle(any(SelectActiveCourseCommand.class));
    }

    @Test
    void shouldChangeTheActiveCourse() throws Exception {
        // Arrange
        var courseId = new CourseId(UUID.randomUUID());
        var command = new SelectActiveCourseCommand("teacher-1", courseId);
        var workspace = TeacherWorkspace.createFor("teacher-1");
        workspace.activate(command);
        when(commandService.handle(command)).thenReturn(workspace);

        // Act & Assert
        mockMvc.perform(put("/api/v1/teachers/me/workspace/active-course").with(user("teacher-1").roles("TEACHER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"courseId\":\"" + courseId.value() + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeCourseId").value(courseId.value().toString()));
    }

    @Test
    void shouldReturnNotFoundForACourseOfAnotherTeacher() throws Exception {
        when(commandService.handle(any(SelectActiveCourseCommand.class)))
                .thenThrow(new CourseNotOwnedByTeacherException(UUID.randomUUID()));

        mockMvc.perform(put("/api/v1/teachers/me/workspace/active-course").with(user("teacher-1").roles("TEACHER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"courseId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void shouldReturnBadRequestWithoutCourseId() throws Exception {
        mockMvc.perform(put("/api/v1/teachers/me/workspace/active-course").with(user("teacher-1").roles("TEACHER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectWhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/teachers/me/workspace"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldForbidTheWorkspaceToStudents() throws Exception {
        mockMvc.perform(get("/api/v1/teachers/me/workspace").with(user("student-1").roles("STUDENT")))
                .andExpect(status().isForbidden());
        verify(queryService, never()).handle(any(GetTeacherWorkspaceByHolderIdQuery.class));
    }
}
