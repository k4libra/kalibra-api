package com.kalibra.api.curriculum.interfaces.rest.resources;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record SelectActiveCourseResource(@NotNull UUID courseId) { }
