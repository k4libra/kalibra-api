package com.kalibra.api.progress.domain.model.valueobjects;

import java.util.UUID;

public record AttemptId(UUID value) {

    public AttemptId {
        if (value == null) {
            throw new IllegalArgumentException("AttemptId value cannot be null");
        }
    }
}
