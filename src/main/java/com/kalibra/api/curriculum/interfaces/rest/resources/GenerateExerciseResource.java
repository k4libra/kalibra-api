package com.kalibra.api.curriculum.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record GenerateExerciseResource(
        @NotNull UUID subtopicId,
        @Schema(description = "How many approved exercises are requested", minimum = "1", maximum = "10")
        @Min(1) @Max(10) int quantity
) { }
