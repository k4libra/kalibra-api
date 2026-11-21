package com.kalibra.api.curriculum.domain.exceptions;

import java.util.UUID;

public class SubtopicNotFoundException extends RuntimeException {

    public SubtopicNotFoundException(UUID subtopicId) {
        super("Subtopic not found: " + subtopicId);
    }
}
