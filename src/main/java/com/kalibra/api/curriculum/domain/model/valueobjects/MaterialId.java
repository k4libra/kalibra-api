package com.kalibra.api.curriculum.domain.model.valueobjects;

import java.util.UUID;

public record MaterialId(UUID value) {

    public MaterialId {
        if (value == null) {
            throw new IllegalArgumentException("MaterialId value cannot be null");
        }
    }
}
