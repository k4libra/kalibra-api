package com.kalibra.api.shared.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

// Needed only once a browser frontend calls this API from a different origin
// (e.g. a Vite/React dev server). The httpOnly JWT cookie is not sent by the
// browser cross-origin unless CORS explicitly allows credentials for that origin.
@Configuration
public class CorsConfig {

    private final List<String> allowedOrigins;

    // Comma-separated list, e.g. "http://localhost:5173,https://kalibra.vercel.app".
    public CorsConfig(@Value("${cors.allowed-origin}") String allowedOrigin) {
        this.allowedOrigins = Arrays.stream(allowedOrigin.split(","))
                .map(String::strip)
                .filter(origin -> !origin.isEmpty())
                .toList();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        var config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);   // never "*" combined with allowCredentials
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
