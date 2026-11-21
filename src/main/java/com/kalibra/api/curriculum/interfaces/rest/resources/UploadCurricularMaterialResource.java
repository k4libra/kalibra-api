package com.kalibra.api.curriculum.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record UploadCurricularMaterialResource(
        @Schema(description = "Subtopics of the course this material covers; at least one")
        @NotEmpty List<@NotNull UUID> subtopicIds,
        @NotBlank @Size(max = 255) String fileName,
        @Schema(allowableValues = {"PDF", "PNG", "JPEG"})
        @NotBlank String format
) { }
