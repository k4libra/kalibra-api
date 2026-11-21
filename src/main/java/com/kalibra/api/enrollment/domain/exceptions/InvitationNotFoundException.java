package com.kalibra.api.enrollment.domain.exceptions;

public class InvitationNotFoundException extends RuntimeException {

    public InvitationNotFoundException() {
        super("Invitation not found");
    }
}