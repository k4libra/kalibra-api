package com.kalibra.api.progress.domain.model.events;

import java.util.UUID;

public record MasteryUpdated(
        String holderId,
        UUID subtopicId,
        double probability
) {
}