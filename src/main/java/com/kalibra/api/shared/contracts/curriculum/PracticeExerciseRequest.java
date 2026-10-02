package com.kalibra.api.shared.contracts.curriculum;

import java.util.Optional;
import java.util.UUID;

public record PracticeExerciseRequest(UUID courseId, UUID subtopicId, Optional<Double> masteryProbability) { }
