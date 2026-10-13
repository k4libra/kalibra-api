package com.kalibra.api.curriculum.interfaces.rest.resources;

import com.kalibra.api.curriculum.domain.model.valueobjects.SubtopicStatus;

import java.util.UUID;

public record SubtopicResource(UUID id, String name, int displayOrder, SubtopicStatus status, boolean locked) { }
