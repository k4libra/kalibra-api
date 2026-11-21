package com.kalibra.api.iam.interfaces.rest.resources;

import jakarta.validation.constraints.Email;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import static com.kalibra.api.iam.domain.model.valueobjects.Email.REGEX;

public record SignInResource(
        @NotBlank @Email(regexp = REGEX) String email,
        @NotBlank String password,
        @Schema(allowableValues = {"MOBILE_APP", "WEB_PLATFORM"},
                description = "Optional client: MOBILE_APP admits students only, WEB_PLATFORM teachers and administrators")
        @Pattern(regexp = "MOBILE_APP|WEB_PLATFORM") String application
) { }
