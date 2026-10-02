package com.kalibra.api.shared.config;

import com.kalibra.api.shared.engine.EngineProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
@EnableConfigurationProperties(EngineProperties.class)
public class EngineConfig {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);

    // Synchronous channel to the adaptive engine (mastery estimation needs an immediate answer).
    @Bean
    public RestClient engineRestClient(EngineProperties properties) {
        var requestFactory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder()
                        // HTTP/1.1: the engine's server rejects the cleartext HTTP/2 upgrade the JDK client attempts by default.
                        .version(HttpClient.Version.HTTP_1_1)
                        .connectTimeout(CONNECT_TIMEOUT)
                        .build());
        requestFactory.setReadTimeout(properties.requestTimeout());
        return RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory)
                .build();
    }
}
