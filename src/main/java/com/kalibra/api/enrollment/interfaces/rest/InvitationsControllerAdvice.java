package com.kalibra.api.enrollment.interfaces.rest;

import com.kalibra.api.enrollment.domain.exceptions.CourseNotOwnedByTeacherException;
import com.kalibra.api.enrollment.domain.exceptions.InvitationNotFoundException;
import com.kalibra.api.enrollment.domain.exceptions.InvitationNotPendingException;
import com.kalibra.api.enrollment.domain.exceptions.InvitationNotResendableException;
import com.kalibra.api.enrollment.domain.exceptions.StudentAccountNotFoundException;
import com.kalibra.api.enrollment.domain.exceptions.StudentAlreadyEnrolledException;
import com.kalibra.api.enrollment.domain.exceptions.StudentAlreadyInvitedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = InvitationsController.class)
public class InvitationsControllerAdvice {

    @ExceptionHandler(StudentAccountNotFoundException.class)
    public ProblemDetail handleStudentAccountNotFound(StudentAccountNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    @ExceptionHandler(CourseNotOwnedByTeacherException.class)
    public ProblemDetail handleCourseNotOwned(CourseNotOwnedByTeacherException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(InvitationNotFoundException.class)
    public ProblemDetail handleInvitationNotFound(InvitationNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(InvitationNotPendingException.class)
    public ProblemDetail handleInvitationNotPending(InvitationNotPendingException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(InvitationNotResendableException.class)
    public ProblemDetail handleInvitationNotResendable(InvitationNotResendableException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(StudentAlreadyInvitedException.class)
    public ProblemDetail handleStudentAlreadyInvited(StudentAlreadyInvitedException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(StudentAlreadyEnrolledException.class)
    public ProblemDetail handleStudentAlreadyEnrolled(StudentAlreadyEnrolledException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }
}
