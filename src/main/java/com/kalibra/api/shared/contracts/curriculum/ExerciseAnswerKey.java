package com.kalibra.api.shared.contracts.curriculum;

import java.util.UUID;

public record ExerciseAnswerKey(UUID exerciseId, UUID subtopicId, String statement, String correctOptionKey, String explanation) { }
