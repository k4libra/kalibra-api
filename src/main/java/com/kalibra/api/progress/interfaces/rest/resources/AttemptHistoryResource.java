package com.kalibra.api.progress.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record AttemptHistoryResource(
        int allCount,
        int correctCount,
        int incorrectCount,
        @Schema(allowableValues = {"ALL", "CORRECT", "INCORRECT"})
        String activeFilter,
        List<ExerciseAttemptResource> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
