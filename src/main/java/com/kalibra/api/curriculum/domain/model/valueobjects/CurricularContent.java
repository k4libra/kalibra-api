package com.kalibra.api.curriculum.domain.model.valueobjects;

public record CurricularContent(String normalizedText, int pageCount) {

    public CurricularContent {
        if (normalizedText == null || normalizedText.isBlank()) {
            throw new IllegalArgumentException("Curricular content cannot be blank");
        }
        if (pageCount < 0) {
            throw new IllegalArgumentException("Page count cannot be negative");
        }
    }
}
