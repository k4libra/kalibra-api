package com.kalibra.api.iam.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import static com.kalibra.api.iam.domain.model.valueobjects.Email.REGEX;

public record SignUpResource(
        @NotBlank @Email(regexp = REGEX) String email,
        @NotBlank @Size(min = 8, max = 128) String password,
        @Schema(allowableValues = {"MOBILE_APP", "WEB_PLATFORM"},
                description = "Client the account is created from: MOBILE_APP registers a STUDENT, WEB_PLATFORM a TEACHER")
        @NotBlank @Pattern(regexp = "MOBILE_APP|WEB_PLATFORM") String application
) { }
