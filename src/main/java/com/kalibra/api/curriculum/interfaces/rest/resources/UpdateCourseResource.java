package com.kalibra.api.curriculum.interfaces.rest.resources;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateCourseResource(@Pattern(regexp = "(?s).*\\S.*") @Size(max = 120) String name,
        @Pattern(regexp = "(?s).*\\S.*") @Size(max = 20) String code) { }
