package com.kalibra.api.shared.config;

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
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        // before the /courses/** rule: a student reads their own progress under that prefix
                        .requestMatchers(HttpMethod.GET, "/api/v1/courses/*/student-progress")
                                .hasAnyAuthority("ROLE_STUDENT", "ROLE_TEACHER")
                        .requestMatchers("/api/v1/courses/**", "/api/v1/teachers/**", "/api/v1/course-exercise-catalogs/**")
                                .hasAuthority("ROLE_TEACHER")
                        .requestMatchers("/api/v1/exercise-attempts/**", "/api/v1/practice-exercises/**",
                                "/api/v1/subtopic-masteries/**")
                                .hasAuthority("ROLE_STUDENT")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/student-preferences/me/daily-reminder")
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
                        .anyRequest().authenticated())
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
