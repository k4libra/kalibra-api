package com.kalibra.api.curriculum.domain.model.valueobjects;

import java.util.UUID;

public record CourseId(UUID value) {

    public CourseId {
        if (value == null) {
            throw new IllegalArgumentException("CourseId value cannot be null");
        }
    }
}
