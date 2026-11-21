package com.kalibra.api.curriculum.domain.exceptions;

import java.util.UUID;

public class CourseNotFoundException extends RuntimeException {

    public CourseNotFoundException(UUID courseId) {
        super("Course not found: " + courseId);
    }
}
