package com.kalibra.api.progress.domain.exceptions;

import java.util.UUID;

public class NoIndicatorsAvailableException extends RuntimeException {

    public NoIndicatorsAvailableException(UUID courseId) {
        super("The course has no indicators to export yet: " + courseId);
    }
}
