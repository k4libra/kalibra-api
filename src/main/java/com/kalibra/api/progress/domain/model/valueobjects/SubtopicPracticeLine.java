package com.kalibra.api.progress.domain.model.valueobjects;

import java.util.UUID;

public record SubtopicPracticeLine(
        UUID subtopicId,
        String subtopicName,
        int solved
) {
}
