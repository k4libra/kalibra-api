package com.kalibra.api.curriculum.domain.model.valueobjects;

public record Pagination(int page, int size) {

    public static final int MAX_SIZE = 100;

    public Pagination {
        if (page < 0) {
            throw new IllegalArgumentException("Page cannot be negative");
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new IllegalArgumentException("Page size must be between 1 and " + MAX_SIZE);
        }
    }

    public static Pagination of(int page, int size) {
        return new Pagination(page, size);
    }
}
