package com.kalibra.api.iam.interfaces.rest.resources;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import static com.kalibra.api.iam.domain.model.valueobjects.Email.REGEX;

public record SignInResource(
        @NotBlank @Email(regexp = REGEX) String email,
        @NotBlank String password
) { }
