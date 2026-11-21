package com.kalibra.api.shared.contracts.curriculum;

import java.util.UUID;

public record SubtopicSummary(UUID subtopicId, String name, int displayOrder, String description) {
    public SubtopicSummary(UUID subtopicId, String name, int displayOrder) {
        this(subtopicId, name, displayOrder, null);
    }
}
