package com.kalibra.api.enrollment.interfaces.rest;

import com.kalibra.api.enrollment.domain.model.queries.GetEnrollmentRostersByHolderIdQuery;
import com.kalibra.api.enrollment.domain.model.valueobjects.CourseRosterGroup;
import com.kalibra.api.enrollment.domain.model.valueobjects.RosterLine;
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

@WebMvcTest(CourseRostersController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, EnrollmentAssemblerImpl.class})
@TestPropertySource(properties = "kalibra.jwt.secret=test-secret-only-for-wiring-not-a-real-value")
class CourseRostersControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    EnrollmentQueryService queryService;

    @Test
    void shouldListTheEnrolledStudentsGroupedByCourse() throws Exception {
        var student = new RosterLine(UUID.randomUUID(), "ana@kalibra.pe", Instant.now());
        var algebra = new CourseRosterGroup(UUID.randomUUID(), "Algebra", "MAT101", 1, List.of(student));
        var physics = new CourseRosterGroup(UUID.randomUUID(), "Physics", "FIS101", 0, List.of());
        when(queryService.handle(new GetEnrollmentRostersByHolderIdQuery("teacher-1"))).thenReturn(List.of(algebra, physics));

        mockMvc.perform(get("/api/v1/course-rosters").with(user("teacher-1").roles("TEACHER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].courseName").value("Algebra"))
                .andExpect(jsonPath("$[0].enrolledCount").value(1))
                .andExpect(jsonPath("$[0].students[0].email").value("ana@kalibra.pe"))
                .andExpect(jsonPath("$[1].enrolledCount").value(0))
                .andExpect(jsonPath("$[1].students").isEmpty());
    }

    @Test
    void shouldRejectWhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/course-rosters"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldForbidTheRostersToStudents() throws Exception {
        mockMvc.perform(get("/api/v1/course-rosters").with(user("student-1").roles("STUDENT")))
                .andExpect(status().isForbidden());
        verify(queryService, never()).handle(any(GetEnrollmentRostersByHolderIdQuery.class));
    }
}
