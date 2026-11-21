package com.kalibra.api.curriculum.domain.exceptions;

public class SubtopicLockedForRemovalException extends RuntimeException {

    public SubtopicLockedForRemovalException() {
        super("El subtema ya tiene material; archívalo en lugar de eliminarlo.");
    }
}
