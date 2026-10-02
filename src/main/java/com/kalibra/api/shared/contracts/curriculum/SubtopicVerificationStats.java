package com.kalibra.api.shared.contracts.curriculum;

import java.util.UUID;

public record SubtopicVerificationStats(UUID subtopicId, String subtopicName, int generated, int approved) { }
