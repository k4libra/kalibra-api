package com.kalibra.api.enrollment.domain.exceptions;

public class StudentAccountNotFoundException extends RuntimeException {

    public StudentAccountNotFoundException(String email) {
        super("Student account not found for email: " + email);
    }
}