package com.kalibra.api.curriculum.interfaces.rest;

import com.kalibra.api.curriculum.domain.exceptions.CourseNotOwnedByTeacherException;
import com.kalibra.api.curriculum.domain.exceptions.SubtopicWithoutIngestedMaterialException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = GeneratedExercisesController.class)
public class GeneratedExercisesControllerAdvice {

    @ExceptionHandler(SubtopicWithoutIngestedMaterialException.class)
    public ProblemDetail handleSubtopicWithoutIngestedMaterial(SubtopicWithoutIngestedMaterialException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(CourseNotOwnedByTeacherException.class)
    public ProblemDetail handleCourseNotOwnedByTeacher(CourseNotOwnedByTeacherException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleInvalidRequest(IllegalArgumentException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }
}
