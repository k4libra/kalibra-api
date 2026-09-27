package com.kalibra.api.curriculum.domain.model.valueobjects;

import com.kalibra.api.curriculum.domain.exceptions.UnsupportedMaterialFormatException;

import java.util.Arrays;

public enum MaterialFormat {
    PDF,
    PNG,
    JPEG;

    public static MaterialFormat from(String value) {
        return Arrays.stream(values())
                .filter(format -> format.name().equalsIgnoreCase(value == null ? "" : value.trim()))
                .findFirst()
                .orElseThrow(() -> new UnsupportedMaterialFormatException(value));
    }
}
