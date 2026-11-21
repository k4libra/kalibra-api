package com.kalibra.api.shared.engine;

// The adaptive engine (or one of its AI providers) could not answer now: retrying later may work.
public class EngineUnavailableException extends RuntimeException {

    public EngineUnavailableException(String message) {
        super(message);
    }

    public EngineUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
