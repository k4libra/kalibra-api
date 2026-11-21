package com.kalibra.api.curriculum.application.internal.outboundservices.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "kalibra.storage.r2")
public record R2Properties(
        String accountId,
        String accessKeyId,
        String secretAccessKey,
        String bucket,
        Duration urlValidity
) {

    public boolean isConfigured() {
        return hasText(accountId) && hasText(accessKeyId) && hasText(secretAccessKey) && hasText(bucket);
    }

    public String endpoint() {
        return "https://" + accountId + ".r2.cloudflarestorage.com";
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
