package com.kalibra.api.curriculum.domain.exceptions;

public class MaterialUploadUnavailableException extends RuntimeException {

    public MaterialUploadUnavailableException() {
        super("Material upload is temporarily unavailable.");
    }
}
