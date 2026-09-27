package com.kalibra.api.enrollment.interfaces.rest;

import com.kalibra.api.enrollment.domain.exceptions.CourseNotOwnedByTeacherException;
import com.kalibra.api.enrollment.domain.exceptions.InvitationNotFoundException;
import com.kalibra.api.enrollment.domain.exceptions.InvitationNotPendingException;
import com.kalibra.api.enrollment.domain.exceptions.InvitationNotResendableException;
import com.kalibra.api.enrollment.domain.exceptions.StudentAccountNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class InvitationsControllerAdvice {

    @ExceptionHandler(StudentAccountNotFoundException.class)
    public ProblemDetail handleStudentAccountNotFound(
            StudentAccountNotFoundException exception) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.UNPROCESSABLE_ENTITY,
                exception.getMessage()
        );
    }

    @ExceptionHandler(CourseNotOwnedByTeacherException.class)
    public ProblemDetail handleCourseNotOwned(
            CourseNotOwnedByTeacherException exception) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                exception.getMessage()
        );
    }

    @ExceptionHandler(InvitationNotFoundException.class)
    public ProblemDetail handleInvitationNotFound(
            InvitationNotFoundException exception) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                exception.getMessage()
        );
    }

    @ExceptionHandler(InvitationNotPendingException.class)
    public ProblemDetail handleInvitationNotPending(
            InvitationNotPendingException exception) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                exception.getMessage()
        );
    }

    @ExceptionHandler(InvitationNotResendableException.class)
    public ProblemDetail handleInvitationNotResendable(
            InvitationNotResendableException exception) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                exception.getMessage()
        );
    }
}