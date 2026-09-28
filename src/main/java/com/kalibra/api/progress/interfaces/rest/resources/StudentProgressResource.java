package com.kalibra.api.progress.interfaces.rest.resources;

import java.util.List;
import java.util.UUID;

public record StudentProgressResource(
        UUID studentId,
        UUID courseId,
        boolean hasActivity,
        List<SubtopicProgressResource> subtopics
) {}