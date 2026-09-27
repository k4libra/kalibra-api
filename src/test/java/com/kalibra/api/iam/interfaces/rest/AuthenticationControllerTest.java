package com.kalibra.api.iam.interfaces.rest;

import com.kalibra.api.iam.application.internal.outboundservices.tokens.TokenService;
import com.kalibra.api.iam.domain.exceptions.EmailAlreadyRegisteredException;
import com.kalibra.api.iam.domain.model.aggregates.User;
import com.kalibra.api.iam.domain.model.commands.SignInCommand;
import com.kalibra.api.iam.domain.model.commands.SignUpCommand;
import com.kalibra.api.iam.domain.model.valueobjects.ClientApplication;
import com.kalibra.api.iam.domain.model.valueobjects.Email;
import com.kalibra.api.iam.domain.model.valueobjects.HashedPassword;
import com.kalibra.api.iam.domain.services.UserCommandService;
import com.kalibra.api.iam.interfaces.rest.transform.UserAssemblerImpl;
import com.kalibra.api.shared.config.JwtAuthenticationFilter;
import com.kalibra.api.shared.config.JwtCookieFactory;
import com.kalibra.api.shared.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthenticationController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtCookieFactory.class, UserAssemblerImpl.class})
@TestPropertySource(properties = "kalibra.jwt.secret=test-secret-only-for-wiring-not-a-real-value")
class AuthenticationControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    UserCommandService userCommandService;

    @MockitoBean
    TokenService tokenService;

    private User studentUser() {
        return User.register(new Email("ada@kalibra.com"), new HashedPassword("hashed"), ClientApplication.MOBILE_APP);
    }

    @Test
    void shouldReturnCreatedWithRolesWhenSigningUp() throws Exception {
        // Arrange
        var user = studentUser();
        when(userCommandService.handle(any(SignUpCommand.class))).thenReturn(Optional.of(user));

        // Act & Assert
        mockMvc.perform(post("/api/v1/authentication/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ada@kalibra.com\",\"password\":\"secret-123\",\"application\":\"MOBILE_APP\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("ada@kalibra.com"))
                .andExpect(jsonPath("$.roles", containsInAnyOrder("REGISTERED_USER", "STUDENT")));
    }

    @Test
    void shouldReturnConflictProblemDetailWhenEmailIsAlreadyRegistered() throws Exception {
        // Arrange
        when(userCommandService.handle(any(SignUpCommand.class)))
                .thenThrow(new EmailAlreadyRegisteredException("ada@kalibra.com"));

        // Act & Assert
        mockMvc.perform(post("/api/v1/authentication/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ada@kalibra.com\",\"password\":\"secret-123\",\"application\":\"WEB_PLATFORM\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void shouldReturnBadRequestWhenApplicationIsUnknown() throws Exception {
        mockMvc.perform(post("/api/v1/authentication/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ada@kalibra.com\",\"password\":\"secret-123\",\"application\":\"DESKTOP\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenPasswordIsTooShort() throws Exception {
        mockMvc.perform(post("/api/v1/authentication/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ada@kalibra.com\",\"password\":\"short\",\"application\":\"MOBILE_APP\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldSetHttpOnlyCookieAndReturnAuthenticatedUserWhenSigningIn() throws Exception {
        // Arrange
        var user = studentUser();
        when(userCommandService.handle(any(SignInCommand.class))).thenReturn(Optional.of(user));
        when(tokenService.issueFor(any(), any())).thenReturn("signed-jwt");

        // Act & Assert
        mockMvc.perform(post("/api/v1/authentication/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ada@kalibra.com\",\"password\":\"secret-123\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("token=signed-jwt")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")))
                .andExpect(jsonPath("$.email").value("ada@kalibra.com"))
                .andExpect(jsonPath("$.roles", containsInAnyOrder("REGISTERED_USER", "STUDENT")));
    }

    @Test
    void shouldReturnUnauthorizedProblemDetailWhenCredentialsAreInvalid() throws Exception {
        // Arrange
        when(userCommandService.handle(any(SignInCommand.class))).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(post("/api/v1/authentication/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ada@kalibra.com\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.detail").value("Invalid credentials"));
    }

    @Test
    void shouldReturnBadRequestWhenSignInEmailHasNoTopLevelDomain() throws Exception {
        mockMvc.perform(post("/api/v1/authentication/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@localhost\",\"password\":\"secret-123\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenSignUpEmailHasNoTopLevelDomain() throws Exception {
        mockMvc.perform(post("/api/v1/authentication/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@localhost\",\"password\":\"secret-123\",\"application\":\"MOBILE_APP\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldClearCookieWhenSigningOut() throws Exception {
        mockMvc.perform(post("/api/v1/authentication/sign-out"))
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));
    }
}
