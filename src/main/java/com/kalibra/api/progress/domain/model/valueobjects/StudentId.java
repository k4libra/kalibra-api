package com.kalibra.api.progress.domain.model.valueobjects;

import java.util.UUID;

public record StudentId(UUID value) {

    public StudentId {
        if (value == null) {
            throw new IllegalArgumentException("StudentId value cannot be null");
        }
    }
}
