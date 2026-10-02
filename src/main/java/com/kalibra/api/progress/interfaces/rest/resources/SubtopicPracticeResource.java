package com.kalibra.api.progress.interfaces.rest.resources;

import java.util.UUID;

public record SubtopicPracticeResource(
        UUID subtopicId,
        String subtopicName,
        int solved
) {
}
