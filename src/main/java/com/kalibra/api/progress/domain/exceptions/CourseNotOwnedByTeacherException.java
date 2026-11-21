package com.kalibra.api.progress.domain.exceptions;

import java.util.UUID;

public class CourseNotOwnedByTeacherException extends RuntimeException {

    public CourseNotOwnedByTeacherException(UUID courseId) {
        super("Course not found: " + courseId);
    }
}
