package com.kalibra.api.enrollment.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record SendInvitationResource(
        @NotNull UUID courseId,
        @Schema(description = "Email of a registered student account")
        @NotBlank String studentEmail
) {
}
