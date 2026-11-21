package com.kalibra.api.progress.domain.model.valueobjects;

import java.util.UUID;

public record CourseIndicatorsReport(
        UUID courseId,
        boolean hasActivity,
        AccuracyIndicator accuracy,
        PracticeIndicator practice,
        MasteryEvolutionIndicator evolution,
        VerificationIndicator verification
) {
}
