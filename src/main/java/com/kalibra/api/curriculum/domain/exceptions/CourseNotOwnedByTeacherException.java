package com.kalibra.api.curriculum.domain.exceptions;

import java.util.UUID;

public class CourseNotOwnedByTeacherException extends RuntimeException {

    public CourseNotOwnedByTeacherException(UUID courseId) {
        super("Course not found: " + courseId);
    }
}
