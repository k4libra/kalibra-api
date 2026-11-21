package com.kalibra.api.curriculum.interfaces.rest;

import com.kalibra.api.curriculum.domain.model.queries.GetCurricularExtractionAvailabilityQuery;
import com.kalibra.api.curriculum.domain.model.valueobjects.CurricularExtractionAvailability;
import com.kalibra.api.curriculum.domain.services.CurricularMaterialQueryService;
import com.kalibra.api.curriculum.interfaces.rest.transform.CurricularMaterialAssemblerImpl;
import com.kalibra.api.shared.config.JwtAuthenticationFilter;
import com.kalibra.api.shared.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CurricularExtractionAvailabilityController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, CurricularMaterialAssemblerImpl.class})
@TestPropertySource(properties = "kalibra.jwt.secret=test-secret-only-for-wiring-not-a-real-value")
class CurricularExtractionAvailabilityControllerTest {

    private static final String PATH = "/api/v1/curricular-extraction-availability";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    CurricularMaterialQueryService queryService;

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldReturn200WithAvailabilityWhenAuthenticatedAsTeacher(boolean available) throws Exception {
        when(queryService.handle(new GetCurricularExtractionAvailabilityQuery()))
                .thenReturn(new CurricularExtractionAvailability(available));

        mockMvc.perform(get(PATH).with(user("teacher-1").roles("TEACHER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(available));
    }

    @Test
    void shouldReturn401WhenNotAuthenticated() throws Exception {
        mockMvc.perform(get(PATH)).andExpect(status().isUnauthorized());
        verifyNoInteractions(queryService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"STUDENT", "REGISTERED_USER", "ADMINISTRATOR"})
    void shouldReturn403WhenAuthenticatedWithoutTeacherRole(String role) throws Exception {
        mockMvc.perform(get(PATH).with(user("user-1").roles(role)))
                .andExpect(status().isForbidden());
        verifyNoInteractions(queryService);
    }
}
