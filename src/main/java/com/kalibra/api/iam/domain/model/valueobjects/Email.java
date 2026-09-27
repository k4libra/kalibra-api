package com.kalibra.api.iam.domain.model.valueobjects;

import java.util.regex.Pattern;

public record Email(String value) {

    public static final String REGEX = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$";

    private static final Pattern FORMAT = Pattern.compile(REGEX);

    public Email {
        if (value == null || !FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid email: " + value);
        }
        value = value.toLowerCase();
    }
}
