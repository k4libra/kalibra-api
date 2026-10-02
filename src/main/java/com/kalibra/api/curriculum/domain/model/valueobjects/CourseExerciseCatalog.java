package com.kalibra.api.curriculum.domain.model.valueobjects;

import java.util.List;
import java.util.UUID;

public record CourseExerciseCatalog(UUID courseId, String courseName, List<SubtopicExerciseCount> subtopics) {

    public CourseExerciseCatalog {
        subtopics = List.copyOf(subtopics);
    }
}
