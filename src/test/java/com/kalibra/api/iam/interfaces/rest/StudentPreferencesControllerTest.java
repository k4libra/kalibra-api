package com.kalibra.api.iam.interfaces.rest;

import com.kalibra.api.iam.domain.model.aggregates.StudentPreferences;
import com.kalibra.api.iam.domain.model.commands.UpdateDailyReminderCommand;
import com.kalibra.api.iam.domain.model.commands.UpdateDarkModeCommand;
import com.kalibra.api.iam.domain.model.queries.GetStudentPreferencesByHolderIdQuery;
import com.kalibra.api.iam.domain.services.StudentPreferencesCommandService;
import com.kalibra.api.iam.domain.services.StudentPreferencesQueryService;
import com.kalibra.api.iam.interfaces.rest.transform.StudentPreferencesAssemblerImpl;
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

import java.time.LocalTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StudentPreferencesController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, StudentPreferencesAssemblerImpl.class})
@TestPropertySource(properties = "kalibra.jwt.secret=test-secret-only-for-wiring-not-a-real-value")
class StudentPreferencesControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    StudentPreferencesCommandService commandService;

    @MockitoBean
    StudentPreferencesQueryService queryService;

    @Test
    void shouldReturnStoredPreferencesWhenAuthenticated() throws Exception {
        // Arrange
        var preferences = StudentPreferences.createDefaultFor("holder-123");
        preferences.update(new UpdateDailyReminderCommand("holder-123", true, LocalTime.of(19, 30)));
        preferences.update(new UpdateDarkModeCommand("holder-123", true));
        when(queryService.handle(new GetStudentPreferencesByHolderIdQuery("holder-123")))
                .thenReturn(Optional.of(preferences));

        // Act & Assert
        mockMvc.perform(get("/api/v1/student-preferences/me").with(user("holder-123").roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dailyReminderEnabled").value(true))
                .andExpect(jsonPath("$.dailyReminderTime").value("19:30:00"))
                .andExpect(jsonPath("$.darkMode").value(true));
    }

    @Test
    void shouldReturnDefaultsWithoutPersistingWhenHolderHasNoPreferences() throws Exception {
        // Arrange
        when(queryService.handle(any(GetStudentPreferencesByHolderIdQuery.class))).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(get("/api/v1/student-preferences/me").with(user("holder-123").roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dailyReminderEnabled").value(false))
                .andExpect(jsonPath("$.dailyReminderTime").doesNotExist())
                .andExpect(jsonPath("$.darkMode").value(false));
        verify(commandService, never()).handle(any(UpdateDailyReminderCommand.class));
        verify(commandService, never()).handle(any(UpdateDarkModeCommand.class));
    }

    @Test
    void shouldReturnUnauthorizedWhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/student-preferences/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldUpdateDailyReminderOfTheAuthenticatedHolder() throws Exception {
        // Arrange
        var updated = StudentPreferences.createDefaultFor("holder-123");
        var command = new UpdateDailyReminderCommand("holder-123", true, LocalTime.of(19, 30));
        updated.update(command);
        when(commandService.handle(command)).thenReturn(updated);

        // Act & Assert
        mockMvc.perform(put("/api/v1/student-preferences/me/daily-reminder")
                        .with(user("holder-123").roles("STUDENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":true,\"time\":\"19:30\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dailyReminderEnabled").value(true))
                .andExpect(jsonPath("$.dailyReminderTime").value("19:30:00"));
    }

    @Test
    void shouldReturnBadRequestWhenEnablingReminderWithoutTime() throws Exception {
        mockMvc.perform(put("/api/v1/student-preferences/me/daily-reminder")
                        .with(user("holder-123").roles("STUDENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":true}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldUpdateDarkModeOfTheAuthenticatedHolder() throws Exception {
        // Arrange
        var updated = StudentPreferences.createDefaultFor("holder-123");
        var command = new UpdateDarkModeCommand("holder-123", true);
        updated.update(command);
        when(commandService.handle(command)).thenReturn(updated);

        // Act & Assert
        mockMvc.perform(put("/api/v1/student-preferences/me/dark-mode")
                        .with(user("holder-123").roles("STUDENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.darkMode").value(true));
    }

    @Test
    void shouldRejectUpdateWhenNotAuthenticated() throws Exception {
        mockMvc.perform(put("/api/v1/student-preferences/me/dark-mode")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":true}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldForbidTheDailyReminderToTeachers() throws Exception {
        mockMvc.perform(put("/api/v1/student-preferences/me/daily-reminder")
                        .with(user("teacher-1").roles("TEACHER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":true,\"time\":\"19:30\"}"))
                .andExpect(status().isForbidden());
        verify(commandService, never()).handle(any(UpdateDailyReminderCommand.class));
    }
}
