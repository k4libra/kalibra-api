package com.kalibra.api.curriculum.domain.exceptions;

public class UnsupportedMaterialFormatException extends RuntimeException {

    public UnsupportedMaterialFormatException(String format) {
        super("Unsupported material format: " + format + ". Supported formats: PDF, PNG, JPEG");
    }
}
