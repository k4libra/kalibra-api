package com.kalibra.api.progress.domain.model.valueobjects;

import java.util.UUID;

public record SubtopicMasteryId(UUID value) {

    public SubtopicMasteryId {
        if (value == null) {
            throw new IllegalArgumentException("SubtopicMasteryId value cannot be null");
        }
    }
}
