package com.kalibra.api.shared.engine;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "kalibra.engine")
public record EngineProperties(
        String baseUrl,
        Duration requestTimeout,
        Duration taskTimeout,
        String tasksStream,
        String resultsStream,
        String resultsGroup
) {
}
