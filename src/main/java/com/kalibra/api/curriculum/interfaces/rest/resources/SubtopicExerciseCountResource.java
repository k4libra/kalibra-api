package com.kalibra.api.curriculum.interfaces.rest.resources;

import java.util.UUID;

public record SubtopicExerciseCountResource(UUID subtopicId, String subtopicName, int generated, int approved, int discarded) { }
