package com.kalibra.api.progress.domain.model.valueobjects;

import java.util.Locale;

public enum AttemptResultFilter {
    ALL,
    CORRECT,
    INCORRECT;

    public static AttemptResultFilter from(String value) {
        if (value == null || value.isBlank()) {
            return ALL;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException unsupported) {
            throw new IllegalArgumentException("Unsupported result filter: " + value);
        }
    }
}
