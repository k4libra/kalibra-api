package com.kalibra.api.progress.domain.model.valueobjects;

public record AnonymousStudentCode(String value) {

    public AnonymousStudentCode {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Anonymous student code cannot be blank");
        }
    }
}
