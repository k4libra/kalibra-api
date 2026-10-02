package com.kalibra.api.progress.domain.exceptions;

public class ExerciseNotFoundException extends RuntimeException {

    public ExerciseNotFoundException() {
        super("Exercise not found");
    }

    public ExerciseNotFoundException(String message) {
        super(message);
    }
}
