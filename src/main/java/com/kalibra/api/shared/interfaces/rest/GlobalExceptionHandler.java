package com.kalibra.api.shared.interfaces.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.kalibra.api.shared.engine.EngineTaskFailedException;
import com.kalibra.api.shared.engine.EngineUnavailableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

// Extends ResponseEntityExceptionHandler (not a bare @ExceptionHandler(Exception.class))
// so Spring MVC's own well-known exceptions (malformed JSON, validation errors, wrong
// HTTP method, etc.) keep their correct 4xx status — only exceptions with no more
// specific handler anywhere (this class, or a module's own ControllerAdvice) fall
// through to the generic 500 below. Keeps the response body generic — never the
// exception message or stack trace — while the real cause is logged server-side.
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // The adaptive engine or one of its AI providers is down or not configured: the request is
    // valid and may be retried later, so it is a 503 and never the generic 500.
    @ExceptionHandler({EngineUnavailableException.class, EngineTaskFailedException.class})
    public ProblemDetail handleEngineUnavailable(RuntimeException ex) {
        log.warn("Adaptive engine unavailable: {}", ex.getMessage());
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
                "The adaptive engine could not complete the request; try again later");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Unexpected error");
    }
}
