package com.kalibra.api.curriculum.domain.exceptions;

public class LastActiveSubtopicException extends RuntimeException {

    public LastActiveSubtopicException() {
        super("El curso debe conservar al menos un subtema activo.");
    }
}
