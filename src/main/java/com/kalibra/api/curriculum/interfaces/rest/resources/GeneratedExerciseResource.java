package com.kalibra.api.curriculum.interfaces.rest.resources;

import com.kalibra.api.shared.contracts.curriculum.ExplanationStep;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record GeneratedExerciseResource(
        UUID id,
        UUID subtopicId,
        @Schema(allowableValues = {"STUDENT_PRACTICE", "TEACHER_REQUEST"})
        String origin,
        String statement,
        List<ExerciseOptionResource> options,
        String explanation,
        List<ExplanationStep> explanationSteps,
        String keyIdea,
        @Schema(allowableValues = {"EASY", "MEDIUM", "HARD"})
        String difficulty,
        @Schema(allowableValues = {"APPROVED", "DISCARDED"},
                description = "DISCARDED exercises are kept for review only and are never delivered to students")
        String verdict,
        @Schema(description = "Why the verification discarded the exercise; null when approved")
        String rejectionReason,
        Instant generatedAt
) { }
