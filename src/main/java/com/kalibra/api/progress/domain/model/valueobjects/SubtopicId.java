package com.kalibra.api.progress.domain.model.valueobjects;

import java.util.UUID;

public record SubtopicId(UUID value) {

    public SubtopicId {
        if (value == null) {
            throw new IllegalArgumentException("SubtopicId value cannot be null");
        }
    }
}
