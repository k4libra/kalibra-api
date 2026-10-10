package com.kalibra.api.iam.domain.exceptions;

public class ApplicationAccessDeniedException extends RuntimeException {

    public ApplicationAccessDeniedException(String application) {
        super("This account cannot sign in to " + application);
    }
}
