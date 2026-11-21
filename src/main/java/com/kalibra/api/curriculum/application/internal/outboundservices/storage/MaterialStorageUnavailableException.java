package com.kalibra.api.curriculum.application.internal.outboundservices.storage;

// The storage provider is not configured or did not answer: nothing was stored and it may be retried.
public class MaterialStorageUnavailableException extends RuntimeException {

    public MaterialStorageUnavailableException(String message) {
        super(message);
    }

    public MaterialStorageUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
