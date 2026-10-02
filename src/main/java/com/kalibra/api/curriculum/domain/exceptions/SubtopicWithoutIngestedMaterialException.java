package com.kalibra.api.curriculum.domain.exceptions;

import java.util.UUID;

public class SubtopicWithoutIngestedMaterialException extends RuntimeException {

    public SubtopicWithoutIngestedMaterialException(UUID subtopicId) {
        super("Upload curricular material for the subtopic and wait until it is ready before generating exercises: " + subtopicId);
    }
}
