package com.kalibra.api.curriculum.domain.exceptions;

public class SubtopicLockedForRenameException extends RuntimeException {

    public SubtopicLockedForRenameException() {
        super("El subtema ya tiene material y su nombre no se puede cambiar.");
    }
}
