package com.kalibra.api.progress.interfaces.rest;

import com.kalibra.api.progress.domain.exceptions.ExerciseNotFoundException;
import com.kalibra.api.progress.domain.exceptions.NotEnrolledInCourseException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = PracticeExercisesController.class)
public class PracticeExercisesControllerAdvice {

    @ExceptionHandler(NotEnrolledInCourseException.class)
    public ProblemDetail handle(NotEnrolledInCourseException exception) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN,
                exception.getMessage()
        );
    }

    @ExceptionHandler(ExerciseNotFoundException.class)
    public ProblemDetail handle(ExerciseNotFoundException exception) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                exception.getMessage()
        );
    }
}