package com.kalibra.api.enrollment.domain.exceptions;

public class InvitationNotResendableException extends RuntimeException {

    public InvitationNotResendableException() {
        super("Invitation cannot be resent");
    }
}