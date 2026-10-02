package com.kalibra.api.curriculum.interfaces.rest.resources;

import java.util.List;
import java.util.UUID;

public record CourseExerciseCatalogResource(UUID courseId, String courseName, List<SubtopicExerciseCountResource> subtopics) { }
