package com.kalibra.api.enrollment.domain.exceptions;

public class StudentAlreadyInvitedException extends RuntimeException {

    public StudentAlreadyInvitedException() {
        super("The student already has a pending invitation to this course");
    }
}
