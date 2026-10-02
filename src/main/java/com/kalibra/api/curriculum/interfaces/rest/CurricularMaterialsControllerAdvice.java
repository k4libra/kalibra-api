package com.kalibra.api.curriculum.interfaces.rest;

import com.kalibra.api.curriculum.application.internal.outboundservices.storage.MaterialStorageUnavailableException;
import com.kalibra.api.curriculum.domain.exceptions.CourseNotOwnedByTeacherException;
import com.kalibra.api.curriculum.domain.exceptions.UnsupportedMaterialFormatException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = CurricularMaterialsController.class)
public class CurricularMaterialsControllerAdvice {

    @ExceptionHandler(UnsupportedMaterialFormatException.class)
    public ProblemDetail handleUnsupportedMaterialFormat(UnsupportedMaterialFormatException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNSUPPORTED_MEDIA_TYPE, ex.getMessage());
    }

    @ExceptionHandler(CourseNotOwnedByTeacherException.class)
    public ProblemDetail handleCourseNotOwnedByTeacher(CourseNotOwnedByTeacherException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(MaterialStorageUnavailableException.class)
    public ProblemDetail handleStorageUnavailable(MaterialStorageUnavailableException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
                "The material storage is not available; try again later");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleInvalidMaterial(IllegalArgumentException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }
}
