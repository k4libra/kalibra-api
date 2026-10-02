package com.kalibra.api.shared.contracts.curriculum;

import java.util.List;
import java.util.UUID;

public record ExerciseSnapshot(UUID exerciseId, UUID subtopicId, String statement, List<String> options) { }
