package com.kalibra.api.enrollment.domain.exceptions;

public class InvitationNotPendingException extends RuntimeException {

    public InvitationNotPendingException() {
        super("Invitation is not pending");
    }
}