package com.kalibra.api.enrollment.domain.exceptions;

public class StudentAlreadyEnrolledException extends RuntimeException {

    public StudentAlreadyEnrolledException() {
        super("The student is already enrolled in this course");
    }
}
