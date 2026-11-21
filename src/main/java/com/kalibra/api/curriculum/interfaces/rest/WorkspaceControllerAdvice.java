package com.kalibra.api.curriculum.interfaces.rest;

import com.kalibra.api.curriculum.domain.exceptions.CourseNotOwnedByTeacherException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = WorkspaceController.class)
public class WorkspaceControllerAdvice {

    @ExceptionHandler(CourseNotOwnedByTeacherException.class)
    public ProblemDetail handleCourseNotOwnedByTeacher(CourseNotOwnedByTeacherException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }
}
