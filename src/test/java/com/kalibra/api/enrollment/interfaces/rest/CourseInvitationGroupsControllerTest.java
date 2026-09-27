package com.kalibra.api.enrollment.interfaces.rest;

import com.kalibra.api.enrollment.domain.model.queries.GetSentInvitationsByHolderIdQuery;
import com.kalibra.api.enrollment.domain.model.valueobjects.CourseInvitationsGroup;
import com.kalibra.api.enrollment.domain.model.valueobjects.InvitationLine;
import com.kalibra.api.enrollment.domain.model.valueobjects.InvitationStatus;
import com.kalibra.api.enrollment.domain.services.InvitationQueryService;
import com.kalibra.api.enrollment.interfaces.rest.transform.InvitationAssemblerImpl;
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
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CourseInvitationGroupsController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, InvitationAssemblerImpl.class})
@TestPropertySource(properties = "kalibra.jwt.secret=test-secret-only-for-wiring-not-a-real-value")
class CourseInvitationGroupsControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    InvitationQueryService queryService;

    @Test
    void shouldListTheSentInvitationsGroupedByCourse() throws Exception {
        // Arrange
        var sentAt = Instant.parse("2026-09-01T10:00:00Z");
        var line = new InvitationLine(UUID.randomUUID(), "ana@kalibra.pe", InvitationStatus.PENDING,
                sentAt, sentAt.plusSeconds(3 * 24 * 60 * 60));
        var withInvitations = new CourseInvitationsGroup(UUID.randomUUID(), "Algebra", "MAT101", List.of(line));
        var withoutInvitations = new CourseInvitationsGroup(UUID.randomUUID(), "Physics", "FIS101", List.of());
        when(queryService.handle(new GetSentInvitationsByHolderIdQuery("teacher-1", Optional.empty())))
                .thenReturn(List.of(withInvitations, withoutInvitations));

        // Act & Assert
        mockMvc.perform(get("/api/v1/course-invitation-groups").with(user("teacher-1").roles("TEACHER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].courseCode").value("MAT101"))
                .andExpect(jsonPath("$[0].invitations[0].courseId").value(withInvitations.courseId().toString()))
                .andExpect(jsonPath("$[0].invitations[0].status").value("PENDING"))
                .andExpect(jsonPath("$[0].invitations[0].sentAt").value("2026-09-01T10:00:00Z"))
                .andExpect(jsonPath("$[1].invitations").isEmpty());
    }

    @Test
    void shouldFilterTheInvitationsByAnySupportedStatusIgnoringCase() throws Exception {
        var expired = new CourseInvitationsGroup(UUID.randomUUID(), "Algebra", "MAT101", List.of());
        when(queryService.handle(new GetSentInvitationsByHolderIdQuery("teacher-1", Optional.of(InvitationStatus.EXPIRED))))
                .thenReturn(List.of(expired));

        mockMvc.perform(get("/api/v1/course-invitation-groups").param("status", "expired")
                        .with(user("teacher-1").roles("TEACHER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].courseCode").value("MAT101"));
    }

    @Test
    void shouldReturnBadRequestForAnUnsupportedStatus() throws Exception {
        mockMvc.perform(get("/api/v1/course-invitation-groups").param("status", "OPEN")
                        .with(user("teacher-1").roles("TEACHER")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
        verify(queryService, never()).handle(any(GetSentInvitationsByHolderIdQuery.class));
    }

    @Test
    void shouldRejectWhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/course-invitation-groups"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldForbidTheSentInvitationsToStudents() throws Exception {
        mockMvc.perform(get("/api/v1/course-invitation-groups").with(user("student-1").roles("STUDENT")))
                .andExpect(status().isForbidden());
        verify(queryService, never()).handle(any(GetSentInvitationsByHolderIdQuery.class));
    }
}
