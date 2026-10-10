package com.kalibra.api.shared.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String[] API_DOCS = {"/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html"};

    private final boolean apiDocsEnabled;

    public SecurityConfig(@Value("${springdoc.api-docs.enabled:false}") boolean apiDocsEnabled) {
        this.apiDocsEnabled = apiDocsEnabled;
    }

    @Bean
    public SecurityFilterChain jwtFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtFilter) throws Exception {
        return http
                // CSRF token machinery stays disabled: the JWT now travels in a cookie the
                // browser attaches automatically, so SameSite=Lax on that cookie
                // (JwtCookieFactory) is the CSRF mitigation for this stateless JSON API,
                // not Spring's form-oriented CSRF token filter. See CorsConfig for the
                // cross-origin browser frontend scenario.
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/authentication/**").permitAll()
                        .requestMatchers("/actuator/health").permitAll()   // healthchecks send no JWT
                        // API docs are public only where SWAGGER_ENABLED=true (local); otherwise they need a session
                        // and springdoc does not serve them at all.
                        .requestMatchers(API_DOCS).access((authentication, context) ->
                                new AuthorizationDecision(apiDocsEnabled))
                        .requestMatchers("/api/v1/institutional-indicators/**").hasAuthority("ROLE_ADMINISTRATOR")
                        .requestMatchers("/api/v1/students/me/**").hasAuthority("ROLE_STUDENT")
                        .requestMatchers("/api/v1/users/me").authenticated()
                        .requestMatchers("/api/v1/student-preferences/**").hasAuthority("ROLE_STUDENT")
                        // before the /courses/** rule: a student reads their own progress under that prefix
                        .requestMatchers(HttpMethod.GET, "/api/v1/courses/*/student-progress")
                                .hasAnyAuthority("ROLE_STUDENT", "ROLE_TEACHER")
                        .requestMatchers("/api/v1/courses/**", "/api/v1/teachers/**", "/api/v1/course-exercise-catalogs/**")
                                .hasAuthority("ROLE_TEACHER")
                        .requestMatchers("/api/v1/exercise-attempts/**", "/api/v1/practice-exercises/**",
                                "/api/v1/subtopic-masteries/**")
                                .hasAuthority("ROLE_STUDENT")
                        .requestMatchers(HttpMethod.POST, "/api/v1/invitations",
                                "/api/v1/invitations/*/cancellations", "/api/v1/invitations/*/renewals")
                                .hasAuthority("ROLE_TEACHER")
                        .requestMatchers("/api/v1/course-invitation-groups/**", "/api/v1/course-rosters/**")
                                .hasAuthority("ROLE_TEACHER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/invitations").hasAuthority("ROLE_STUDENT")
                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/invitations/*/acceptances", "/api/v1/invitations/*/rejections")
                                .hasAuthority("ROLE_STUDENT")
                        .anyRequest().denyAll())
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                        // sendError(403) would re-dispatch to /error, where the JWT filter does not
                        // run, and the request would end up answered as 401.
                        .accessDeniedHandler((request, response, denied) ->
                                response.setStatus(HttpStatus.FORBIDDEN.value())))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
