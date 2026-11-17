package com.kalibra.api.progress.interfaces.rest.resources;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.kalibra.api.progress.domain.model.valueobjects.PracticeMode;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RequestPracticeExerciseResource(
        @NotNull UUID courseId,
        UUID subtopicId,
        PracticeMode mode
) {
    @AssertTrue(message = "Indica un subtema o usa el modo EXPRESS sin subtema")
    @JsonIgnore
    public boolean isSelectionValid() {
        return mode == null ? subtopicId != null : subtopicId == null;
    }
}
