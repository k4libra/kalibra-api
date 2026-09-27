package com.kalibra.api.curriculum.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

public record TeacherWorkspaceResource(
        @Schema(description = "Active course of the teacher; null until one is selected")
        UUID activeCourseId
) { }
