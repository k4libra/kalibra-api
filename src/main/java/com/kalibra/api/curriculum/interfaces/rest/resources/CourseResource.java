package com.kalibra.api.curriculum.interfaces.rest.resources;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CourseResource(UUID id, String name, String code, List<SubtopicResource> subtopics, Instant createdAt) { }
