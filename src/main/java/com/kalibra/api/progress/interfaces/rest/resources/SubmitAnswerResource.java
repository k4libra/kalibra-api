package com.kalibra.api.progress.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public record SubmitAnswerResource(
        @NotNull UUID courseId,
        @NotNull UUID exerciseId,
        @Schema(description = "Key of the chosen option; the options of a practice exercise come in order A, B, C, D",
                allowableValues = {"A", "B", "C", "D"})
        @NotNull @Pattern(regexp = "[A-Da-d]") String selectedOptionKey
) {
}
