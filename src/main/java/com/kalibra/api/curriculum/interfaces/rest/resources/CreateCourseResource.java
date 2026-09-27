package com.kalibra.api.curriculum.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateCourseResource(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(max = 20) String code,
        @Schema(description = "Subtopic names in display order; at least one is required")
        List<@NotBlank @Size(max = 120) String> subtopicNames
) { }
