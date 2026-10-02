package com.kalibra.api.curriculum.domain.exceptions;

public class UnsupportedMaterialFormatException extends RuntimeException {

    public UnsupportedMaterialFormatException(String format) {
        this("Unsupported material format: " + format + ". Supported formats: PDF, PNG, JPEG", null);
    }

    private UnsupportedMaterialFormatException(String message, Throwable cause) {
        super(message, cause);
    }

    public static UnsupportedMaterialFormatException unreadable(String format) {
        return new UnsupportedMaterialFormatException("The file is corrupt or is not a valid " + format + " file", null);
    }
}
