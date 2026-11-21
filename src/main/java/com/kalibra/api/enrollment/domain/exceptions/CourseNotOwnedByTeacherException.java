package com.kalibra.api.enrollment.domain.exceptions;

public class CourseNotOwnedByTeacherException extends RuntimeException {

    public CourseNotOwnedByTeacherException() {
        super("Course is not owned by the authenticated teacher");
    }
}