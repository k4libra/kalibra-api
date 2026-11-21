package com.kalibra.api.curriculum.interfaces.rest.resources;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSubtopicResource(@NotBlank @Size(max = 120) String name,
        @Size(max = 280, message = "La descripción del subtema no puede superar los 280 caracteres") String description) { }
