package com.kalibra.api.curriculum.interfaces.rest.resources;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddSubtopicResource(@NotBlank @Size(max = 120) String name) { }
