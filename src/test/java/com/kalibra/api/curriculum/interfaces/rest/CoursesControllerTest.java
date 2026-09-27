package com.kalibra.api.curriculum.interfaces.rest;

import com.kalibra.api.curriculum.domain.exceptions.CourseWithoutSubtopicsException;
import com.kalibra.api.curriculum.domain.model.aggregates.Course;
import com.kalibra.api.curriculum.domain.model.commands.CreateCourseCommand;
import com.kalibra.api.curriculum.domain.model.queries.GetCourseByIdQuery;
import com.kalibra.api.curriculum.domain.model.queries.GetCoursesByHolderIdQuery;
import com.kalibra.api.curriculum.domain.model.valueobjects.CourseCode;
import com.kalibra.api.curriculum.domain.services.CourseCommandService;
import com.kalibra.api.curriculum.domain.services.CourseQueryService;
import com.kalibra.api.curriculum.interfaces.rest.transform.CourseAssemblerImpl;
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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
}
