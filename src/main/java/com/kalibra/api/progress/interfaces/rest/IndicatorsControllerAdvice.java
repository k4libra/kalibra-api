package com.kalibra.api.progress.interfaces.rest;

import com.kalibra.api.progress.domain.exceptions.CourseNotOwnedByTeacherException;
import com.kalibra.api.progress.domain.exceptions.NoIndicatorsAvailableException;
import com.kalibra.api.progress.domain.exceptions.NotEnrolledInCourseException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = IndicatorsController.class)
public class IndicatorsControllerAdvice {

    @ExceptionHandler(CourseNotOwnedByTeacherException.class)
    public ProblemDetail handle(CourseNotOwnedByTeacherException exception) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                exception.getMessage()
        );
    }

    @ExceptionHandler(NoIndicatorsAvailableException.class)
    public ProblemDetail handle(NoIndicatorsAvailableException exception) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                exception.getMessage()
        );
    }

    @ExceptionHandler(NotEnrolledInCourseException.class)
    public ProblemDetail handle(NotEnrolledInCourseException exception) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN,
                exception.getMessage()
        );
    }
}
