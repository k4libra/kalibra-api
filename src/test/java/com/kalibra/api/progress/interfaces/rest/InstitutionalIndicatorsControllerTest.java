package com.kalibra.api.progress.interfaces.rest;

import com.kalibra.api.progress.domain.model.queries.GetCriticalSubtopicsQuery;
import com.kalibra.api.progress.domain.model.queries.GetInstitutionalIndicatorsQuery;
import com.kalibra.api.progress.domain.model.valueobjects.CourseIndicatorSummary;
import com.kalibra.api.progress.domain.model.valueobjects.CriticalSubtopicLine;
import com.kalibra.api.progress.domain.model.valueobjects.InstitutionalIndicatorTotals;
import com.kalibra.api.progress.domain.model.valueobjects.InstitutionalIndicators;
import com.kalibra.api.progress.domain.services.InstitutionalIndicatorsQueryService;
import com.kalibra.api.progress.interfaces.rest.transform.InstitutionalIndicatorsAssemblerImpl;
import com.kalibra.api.shared.config.JwtAuthenticationFilter;
import com.kalibra.api.shared.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InstitutionalIndicatorsController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, InstitutionalIndicatorsAssemblerImpl.class})
@TestPropertySource(properties = "kalibra.jwt.secret=test-secret-only-for-wiring-not-a-real-value")
class InstitutionalIndicatorsControllerTest {

    private static final String PATH = "/api/v1/institutional-indicators";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    InstitutionalIndicatorsQueryService queryService;

    private InstitutionalIndicators indicators() {
        var course = new CourseIndicatorSummary(UUID.randomUUID(), "Algebra", "MAT101", "Profesor Test Uno", true,
                3, 2, 12, 75.0, 18, 90.0);
        return new InstitutionalIndicators(new InstitutionalIndicatorTotals(1, 1, 3, 2, 12, 75.0, 18, 90.0),
                List.of(course));
    }

    @Test
    void shouldReturnTheInstitutionalIndicatorsToAdministrators() throws Exception {
        when(queryService.handle(new GetInstitutionalIndicatorsQuery())).thenReturn(indicators());

        mockMvc.perform(get(PATH).with(user("admin-1").roles("ADMINISTRATOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totals.courses").value(1))
                .andExpect(jsonPath("$.totals.groupAccuracy").value(75.0))
                .andExpect(jsonPath("$.courses[0].teacherName").value("Profesor Test Uno"))
                .andExpect(jsonPath("$.courses[0].verificationApprovalRate").value(90.0));
    }

    @Test
    void shouldExportCourseAggregatesAsCsvWithoutIdentities() throws Exception {
        when(queryService.handle(new GetInstitutionalIndicatorsQuery())).thenReturn(indicators());

        mockMvc.perform(get(PATH).accept("text/csv").with(user("admin-1").roles("ADMINISTRATOR")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"MAT101\",\"Algebra\",3,2,12,75.0,18,90.0")))
                .andExpect(content().string(not(containsString("Profesor"))));
    }

    @Test
    void shouldListTheCriticalSubtopics() throws Exception {
        var line = new CriticalSubtopicLine(UUID.randomUUID(), "Algebra", UUID.randomUUID(), "Vectores",
                31.0, 2, 1, 0, 0);
        when(queryService.handle(new GetCriticalSubtopicsQuery())).thenReturn(List.of(line));

        mockMvc.perform(get(PATH + "/critical-subtopics").with(user("admin-1").roles("ADMINISTRATOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].subtopicName").value("Vectores"))
                .andExpect(jsonPath("$[0].groupMastery").value(31.0));
    }

    @Test
    void shouldRejectWhenNotAuthenticated() throws Exception {
        mockMvc.perform(get(PATH)).andExpect(status().isUnauthorized());
    }

    @Test
    void shouldForbidTheIndicatorsToTeachersAndStudents() throws Exception {
        mockMvc.perform(get(PATH).with(user("teacher-1").roles("TEACHER"))).andExpect(status().isForbidden());
        mockMvc.perform(get(PATH + "/critical-subtopics").with(user("student-1").roles("STUDENT")))
                .andExpect(status().isForbidden());
        verify(queryService, never()).handle(any(GetInstitutionalIndicatorsQuery.class));
    }

    @Test
    void shouldForbidTeacherEndpointsToAdministrators() throws Exception {
        mockMvc.perform(get("/api/v1/courses").with(user("admin-1").roles("ADMINISTRATOR")))
                .andExpect(status().isForbidden());
    }
}
