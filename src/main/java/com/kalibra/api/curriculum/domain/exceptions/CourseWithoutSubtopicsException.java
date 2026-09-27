package com.kalibra.api.curriculum.domain.exceptions;

public class CourseWithoutSubtopicsException extends RuntimeException {

    public CourseWithoutSubtopicsException() {
        super("A course needs at least one subtopic");
    }
}
