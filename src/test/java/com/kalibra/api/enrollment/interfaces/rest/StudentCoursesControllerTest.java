package com.kalibra.api.enrollment.interfaces.rest;

import com.kalibra.api.enrollment.domain.model.queries.GetEnrolledCoursesByStudentQuery;
import com.kalibra.api.enrollment.domain.model.valueobjects.EnrolledCourse;
import com.kalibra.api.enrollment.domain.model.valueobjects.StudentId;
import com.kalibra.api.enrollment.domain.services.EnrollmentQueryService;
import com.kalibra.api.enrollment.interfaces.rest.transform.EnrollmentAssemblerImpl;
import com.kalibra.api.shared.config.JwtAuthenticationFilter;
import com.kalibra.api.shared.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StudentCoursesController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, EnrollmentAssemblerImpl.class})
@TestPropertySource(properties = "kalibra.jwt.secret=test-secret-only-for-wiring-not-a-real-value")
class StudentCoursesControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    EnrollmentQueryService queryService;

    @Test
    void shouldListTheCoursesOfTheStudentWithTheirSubtopics() throws Exception {
        var studentId = UUID.randomUUID();
        var course = new EnrolledCourse(UUID.randomUUID(), "Algebra", "MAT101", "Profesor Test Uno",
                List.of(new EnrolledCourse.EnrolledSubtopic(UUID.randomUUID(), "Vectores", 1)), Instant.now());
        when(queryService.handle(new GetEnrolledCoursesByStudentQuery(new StudentId(studentId))))
                .thenReturn(List.of(course));

        mockMvc.perform(get("/api/v1/students/me/courses").with(user(studentId.toString()).roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Algebra"))
                .andExpect(jsonPath("$[0].teacherName").value("Profesor Test Uno"))
                .andExpect(jsonPath("$[0].subtopics[0].name").value("Vectores"))
                .andExpect(jsonPath("$[0].subtopics[0].displayOrder").value(1));
    }

    @Test
    void shouldRejectWhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/students/me/courses"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldForbidTheCoursesToTeachersAndAdministrators() throws Exception {
        mockMvc.perform(get("/api/v1/students/me/courses").with(user(UUID.randomUUID().toString()).roles("TEACHER")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/students/me/courses").with(user(UUID.randomUUID().toString()).roles("ADMINISTRATOR")))
                .andExpect(status().isForbidden());
        verify(queryService, never()).handle(any(GetEnrolledCoursesByStudentQuery.class));
    }
}
