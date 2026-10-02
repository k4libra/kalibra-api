package com.kalibra.api.progress.domain.model.valueobjects;

import java.util.UUID;

public record PracticeSubtopicView(
        UUID subtopicId,
        String name,
        MasteryLevel level
) {}