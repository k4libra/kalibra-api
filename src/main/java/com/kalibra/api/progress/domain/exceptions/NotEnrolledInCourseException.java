package com.kalibra.api.progress.domain.exceptions;

public class NotEnrolledInCourseException extends RuntimeException {

    public NotEnrolledInCourseException() {
        super("Student is not enrolled in the course");
    }
}