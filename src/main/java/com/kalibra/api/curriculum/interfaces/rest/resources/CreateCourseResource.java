package com.kalibra.api.curriculum.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateCourseResource(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(max = 20) String code,
        @Schema(description = "Legacy subtopic names in display order, used when subtopics is omitted")
        List<@NotBlank @Size(max = 120) String> subtopicNames,
        String term, String faculty, String semester,
        List<@Valid @NotNull CreateSubtopicResource> subtopics
) { }
