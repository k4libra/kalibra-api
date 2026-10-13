package com.kalibra.api.curriculum.interfaces.rest;

import com.kalibra.api.curriculum.domain.exceptions.CourseNotFoundException;
import com.kalibra.api.curriculum.domain.exceptions.CourseWithoutSubtopicsException;
import com.kalibra.api.curriculum.domain.exceptions.LastActiveSubtopicException;
import com.kalibra.api.curriculum.domain.exceptions.SubtopicLockedForRemovalException;
import com.kalibra.api.curriculum.domain.exceptions.SubtopicLockedForRenameException;
import com.kalibra.api.curriculum.domain.exceptions.SubtopicNotFoundException;
import com.kalibra.api.curriculum.domain.model.aggregates.Course;
import com.kalibra.api.curriculum.domain.model.commands.AddSubtopicCommand;
import com.kalibra.api.curriculum.domain.model.commands.CreateCourseCommand;
import com.kalibra.api.curriculum.domain.model.commands.RemoveSubtopicCommand;
import com.kalibra.api.curriculum.domain.model.commands.UpdateCourseCommand;
import com.kalibra.api.curriculum.domain.model.commands.UpdateSubtopicCommand;
import com.kalibra.api.curriculum.domain.model.queries.GetCourseByIdQuery;
import com.kalibra.api.curriculum.domain.model.queries.GetCoursesByHolderIdQuery;
import com.kalibra.api.curriculum.domain.model.queries.GetLockedSubtopicIdsByCourseQuery;
import com.kalibra.api.curriculum.domain.model.valueobjects.CourseCode;
import com.kalibra.api.curriculum.domain.model.valueobjects.SubtopicStatus;
import com.kalibra.api.curriculum.domain.services.CourseCommandService;
import com.kalibra.api.curriculum.domain.services.CourseQueryService;
import com.kalibra.api.curriculum.interfaces.rest.transform.CourseAssemblerImpl;
import com.kalibra.api.shared.config.JwtAuthenticationFilter;
import com.kalibra.api.shared.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CoursesController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, CourseAssemblerImpl.class})
@TestPropertySource(properties = "kalibra.jwt.secret=test-secret-only-for-wiring-not-a-real-value")
class CoursesControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    CourseCommandService commandService;

    @MockitoBean
    CourseQueryService queryService;

    private final Course course = Course.create(new CreateCourseCommand("teacher-1", "Algebra",
            new CourseCode("MAT101"), List.of("Equations", "Inequalities")));

    @Test
    void shouldCreateCourseForTheAuthenticatedTeacher() throws Exception {
        // Arrange
        when(commandService.handle(new CreateCourseCommand("teacher-1", "Algebra", new CourseCode("MAT101"),
                List.of("Equations", "Inequalities")))).thenReturn(course);

        // Act & Assert
        mockMvc.perform(post("/api/v1/courses").with(user("teacher-1").roles("TEACHER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Algebra\",\"code\":\"MAT101\",\"subtopicNames\":[\"Equations\",\"Inequalities\"]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("MAT101"))
                .andExpect(jsonPath("$.subtopics[0].name").value("Equations"))
                .andExpect(jsonPath("$.subtopics[1].displayOrder").value(2));
    }

    @Test
    void shouldReturnUnprocessableEntityWhenTheCourseHasNoSubtopics() throws Exception {
        // Arrange
        when(commandService.handle(any(CreateCourseCommand.class))).thenThrow(new CourseWithoutSubtopicsException());

        // Act & Assert
        mockMvc.perform(post("/api/v1/courses").with(user("teacher-1").roles("TEACHER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Algebra\",\"code\":\"MAT101\",\"subtopicNames\":[]}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422));
    }

    @Test
    void shouldReturnBadRequestWhenTheCodeIsTooLong() throws Exception {
        mockMvc.perform(post("/api/v1/courses").with(user("teacher-1").roles("TEACHER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Algebra\",\"code\":\"ABCDEFGHIJKLMNOPQRSTU\",\"subtopicNames\":[\"Equations\"]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldListOnlyTheCoursesOfTheTeacher() throws Exception {
        when(queryService.handle(new GetCoursesByHolderIdQuery("teacher-1"))).thenReturn(List.of(course));

        mockMvc.perform(get("/api/v1/courses").with(user("teacher-1").roles("TEACHER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Algebra"));
    }

    @Test
    void shouldReturnOwnCourseWithItsSubtopics() throws Exception {
        when(queryService.handle(new GetCourseByIdQuery(course.getId()))).thenReturn(Optional.of(course));

        mockMvc.perform(get("/api/v1/courses/{id}", course.getId().value()).with(user("teacher-1").roles("TEACHER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtopics.length()").value(2));
    }

    @Test
    void shouldReturnNotFoundForTheCourseOfAnotherTeacher() throws Exception {
        when(queryService.handle(new GetCourseByIdQuery(course.getId()))).thenReturn(Optional.of(course));

        mockMvc.perform(get("/api/v1/courses/{id}", course.getId().value()).with(user("teacher-2").roles("TEACHER")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void shouldReturnNotFoundForAnUnknownCourse() throws Exception {
        when(queryService.handle(any(GetCourseByIdQuery.class))).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/courses/{id}", UUID.randomUUID()).with(user("teacher-1").roles("TEACHER")))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRejectWhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/courses"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldForbidCoursesToStudents() throws Exception {
        mockMvc.perform(post("/api/v1/courses").with(user("student-1").roles("STUDENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Algebra\",\"code\":\"MAT101\",\"subtopicNames\":[\"Equations\"]}"))
                .andExpect(status().isForbidden());
        verify(commandService, never()).handle(any(CreateCourseCommand.class));
    }

    private MockHttpServletRequestBuilder editingRequest(String operation) {
        var coursePath = "/api/v1/courses/" + course.getId().value();
        var subtopicPath = coursePath + "/subtopics/" + course.getSubtopics().getFirst().getId().value();
        return switch (operation) {
            case "course" -> patch(coursePath).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Geometry\"}");
            case "add" -> post(coursePath + "/subtopics").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Limits\"}");
            case "update" -> patch(subtopicPath).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ARCHIVED\"}");
            case "remove" -> delete(subtopicPath);
            default -> throw new AssertionError(operation);
        };
    }

    @ParameterizedTest
    @ValueSource(strings = {"course", "add", "update", "remove"})
    void shouldRejectUnauthenticatedEditing(String operation) throws Exception {
        mockMvc.perform(editingRequest(operation)).andExpect(status().isUnauthorized());
        verifyNoInteractions(commandService, queryService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"course", "add", "update", "remove"})
    void shouldForbidEditingToStudents(String operation) throws Exception {
        mockMvc.perform(editingRequest(operation).with(user("student").roles("STUDENT"))).andExpect(status().isForbidden());
        verifyNoInteractions(commandService, queryService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"course", "add", "update", "remove"})
    void shouldReturn404ForMissingOrOtherTeachersCourseOnEditing(String operation) throws Exception {
        var missing = new CourseNotFoundException(course.getId().value());
        switch (operation) {
            case "course" -> when(commandService.handle(any(UpdateCourseCommand.class))).thenThrow(missing);
            case "add" -> when(commandService.handle(any(AddSubtopicCommand.class))).thenThrow(missing);
            case "update" -> when(commandService.handle(any(UpdateSubtopicCommand.class))).thenThrow(missing);
            case "remove" -> doThrow(missing).when(commandService).handle(any(RemoveSubtopicCommand.class));
            default -> throw new AssertionError(operation);
        }
        mockMvc.perform(editingRequest(operation).with(user("teacher-2").roles("TEACHER")))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void shouldPatchCourseWithAbsentCodePreservedInCommand() throws Exception {
        when(commandService.handle(any(UpdateCourseCommand.class))).thenReturn(course);
        when(queryService.handle(new GetLockedSubtopicIdsByCourseQuery(course.getId(), "teacher-1")))
                .thenReturn(Set.of(course.getSubtopics().getFirst().getId()));
        mockMvc.perform(editingRequest("course").with(user("teacher-1").roles("TEACHER")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.subtopics[0].locked").value(true))
                .andExpect(jsonPath("$.subtopics[0].status").value("ACTIVE"));
        verify(commandService).handle(argThat((UpdateCourseCommand command) -> command.holderId().equals("teacher-1")
                && command.name().equals(Optional.of("Geometry")) && command.code().isEmpty()));
    }

    @Test
    void shouldAddSubtopicAndReturn201() throws Exception {
        when(commandService.handle(any(AddSubtopicCommand.class))).thenReturn(course);
        mockMvc.perform(editingRequest("add").with(user("teacher-1").roles("TEACHER")))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.subtopics[0].status").value("ACTIVE"));
        verify(commandService).handle(new AddSubtopicCommand(course.getId(), "teacher-1", "Limits"));
    }

    @Test
    void shouldPatchSubtopicStatusAndPreserveMissingFields() throws Exception {
        course.getSubtopics().getFirst().setStatus(SubtopicStatus.ARCHIVED);
        when(commandService.handle(any(UpdateSubtopicCommand.class))).thenReturn(course);
        mockMvc.perform(editingRequest("update").with(user("teacher-1").roles("TEACHER")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.subtopics[0].status").value("ARCHIVED"));
        verify(commandService).handle(argThat((UpdateSubtopicCommand command) -> command.status().equals(Optional.of(SubtopicStatus.ARCHIVED))
                && command.name().isEmpty() && command.displayOrder().isEmpty()));
    }

    @Test
    void shouldDeleteSubtopicAndReturn204() throws Exception {
        mockMvc.perform(editingRequest("remove").with(user("teacher-1").roles("TEACHER"))).andExpect(status().isNoContent());
        verify(commandService).handle(new RemoveSubtopicCommand(course.getId(), "teacher-1", course.getSubtopics().getFirst().getId()));
    }

    @Test
    void shouldReturn409ForLockedRenameWithContractDetail() throws Exception {
        when(commandService.handle(any(UpdateSubtopicCommand.class))).thenThrow(new SubtopicLockedForRenameException());
        mockMvc.perform(patch("/api/v1/courses/{id}/subtopics/{subtopic}", course.getId().value(), course.getSubtopics().getFirst().getId().value())
                        .with(user("teacher-1").roles("TEACHER")).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Renamed\"}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.detail").value("El subtema ya tiene material y su nombre no se puede cambiar."));
    }

    @Test
    void shouldReturn409ForLockedRemovalWithContractDetail() throws Exception {
        doThrow(new SubtopicLockedForRemovalException()).when(commandService).handle(any(RemoveSubtopicCommand.class));
        mockMvc.perform(editingRequest("remove").with(user("teacher-1").roles("TEACHER")))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.detail").value("El subtema ya tiene material; archívalo en lugar de eliminarlo."));
    }

    @ParameterizedTest
    @ValueSource(strings = {"update", "remove"})
    void shouldReturn409WhenEditingWouldLeaveNoActiveSubtopic(String operation) throws Exception {
        if (operation.equals("update")) {
            when(commandService.handle(any(UpdateSubtopicCommand.class))).thenThrow(new LastActiveSubtopicException());
        } else {
            doThrow(new LastActiveSubtopicException()).when(commandService).handle(any(RemoveSubtopicCommand.class));
        }
        mockMvc.perform(editingRequest(operation).with(user("teacher-1").roles("TEACHER")))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.detail").value("El curso debe conservar al menos un subtema activo."));
    }

    @ParameterizedTest
    @ValueSource(strings = {"update", "remove"})
    void shouldReturn404ForUnknownSubtopic(String operation) throws Exception {
        var missing = new SubtopicNotFoundException(UUID.randomUUID());
        if (operation.equals("update")) {
            when(commandService.handle(any(UpdateSubtopicCommand.class))).thenThrow(missing);
        } else {
            doThrow(missing).when(commandService).handle(any(RemoveSubtopicCommand.class));
        }
        mockMvc.perform(editingRequest(operation).with(user("teacher-1").roles("TEACHER"))).andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"name\":\" \"}", "{\"code\":\"\"}", "{\"code\":\"ABCDEFGHIJKLMNOPQRSTU\"}"})
    void shouldValidateCoursePatchLikeCreation(String body) throws Exception {
        mockMvc.perform(patch("/api/v1/courses/{id}", course.getId().value()).with(user("teacher-1").roles("TEACHER"))
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        verifyNoInteractions(commandService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"name\":\"\"}", "{\"displayOrder\":0}", "{\"status\":\"UNKNOWN\"}"})
    void shouldRejectInvalidSubtopicPatch(String body) throws Exception {
        mockMvc.perform(patch("/api/v1/courses/{id}/subtopics/{subtopic}", course.getId().value(), course.getSubtopics().getFirst().getId().value())
                .with(user("teacher-1").roles("TEACHER")).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        verifyNoInteractions(commandService);
    }

    @Test
    void shouldListAllSubtopicsIncludingArchivedAndResolveLocksOncePerCourse() throws Exception {
        course.getSubtopics().getFirst().setStatus(SubtopicStatus.ARCHIVED);
        when(queryService.handle(new GetCoursesByHolderIdQuery("teacher-1"))).thenReturn(List.of(course));
        when(queryService.handle(new GetLockedSubtopicIdsByCourseQuery(course.getId(), "teacher-1")))
                .thenReturn(Set.of(course.getSubtopics().getFirst().getId()));
        mockMvc.perform(get("/api/v1/courses").with(user("teacher-1").roles("TEACHER")))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].subtopics.length()").value(2))
                .andExpect(jsonPath("$[0].subtopics[0].status").value("ARCHIVED"))
                .andExpect(jsonPath("$[0].subtopics[0].locked").value(true))
                .andExpect(jsonPath("$[0].subtopics[1].locked").value(false));
        verify(queryService).handle(new GetLockedSubtopicIdsByCourseQuery(course.getId(), "teacher-1"));
    }
}
