package com.kalibra.api.curriculum.interfaces.rest;

import com.kalibra.api.curriculum.domain.exceptions.CourseNotFoundException;
import com.kalibra.api.curriculum.domain.exceptions.CourseWithoutSubtopicsException;
import com.kalibra.api.curriculum.domain.exceptions.LastActiveSubtopicException;
import com.kalibra.api.curriculum.domain.exceptions.SubtopicLockedForRemovalException;
import com.kalibra.api.curriculum.domain.exceptions.SubtopicLockedForRenameException;
import com.kalibra.api.curriculum.domain.exceptions.SubtopicNotFoundException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = CoursesController.class)
public class CoursesControllerAdvice {

    @ExceptionHandler(CourseWithoutSubtopicsException.class)
    public ProblemDetail handleCourseWithoutSubtopics(CourseWithoutSubtopicsException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    @ExceptionHandler(CourseNotFoundException.class)
    public ProblemDetail handleCourseNotFound(CourseNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }
    @ExceptionHandler(SubtopicNotFoundException.class)
    public ProblemDetail handleSubtopicNotFound(SubtopicNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler({SubtopicLockedForRenameException.class, SubtopicLockedForRemovalException.class,
            LastActiveSubtopicException.class})
    public ProblemDetail handleSubtopicConflict(RuntimeException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleInvalidRequest(IllegalArgumentException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }
}
