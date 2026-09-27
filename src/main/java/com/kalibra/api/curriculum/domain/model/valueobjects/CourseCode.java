package com.kalibra.api.curriculum.domain.model.valueobjects;

public record CourseCode(String value) {

    public static final int MAX_LENGTH = 20;

    public CourseCode {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Course code cannot be blank");
        }
        value = value.trim();
        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("Course code cannot exceed " + MAX_LENGTH + " characters");
        }
    }
}
