package com.kalibra.api.curriculum.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CurricularMaterialResource(
        UUID id,
        UUID courseId,
        List<UUID> subtopicIds,
        String fileName,
        String format,
        @Schema(allowableValues = {"PENDING_INGESTION", "READY", "INGESTION_ERROR"},
                description = "READY means exercises can be generated from it; INGESTION_ERROR comes with failureReason")
        String status,
        String failureReason,
        Instant uploadedAt
) { }
