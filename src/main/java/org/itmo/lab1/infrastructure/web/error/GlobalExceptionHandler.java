package org.itmo.lab1.infrastructure.web.error;

import org.itmo.lab1.application.auth.InvalidCredentialsException;
import org.itmo.lab1.application.port.out.PasswordHasherBusyException;
import org.itmo.lab1.application.port.out.UnknownAuthorException;
import org.itmo.lab1.domain.DomainValidationException;
import org.itmo.lab1.infrastructure.security.jwt.InvalidBearerTokenException;
import org.itmo.lab1.infrastructure.web.limit.RequestBodyTooLargeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ProblemDetail> handleAuthentication(AuthenticationException exception) {
        log.debug("Authentication failed: {}", exception.getClass().getSimpleName());
        return unauthorized("Bearer");
    }

    @ExceptionHandler(InvalidBearerTokenException.class)
    ResponseEntity<ProblemDetail> handleInvalidToken(InvalidBearerTokenException exception) {
        return unauthorized("Bearer error=\"invalid_token\"");
    }

    @ExceptionHandler(UnknownAuthorException.class)
    ResponseEntity<ProblemDetail> handleUnknownAuthor(UnknownAuthorException exception) {
        log.warn("Valid token refers to a non-existent user");
        return unauthorized("Bearer error=\"invalid_token\"");
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    ResponseEntity<ProblemDetail> handleInvalidCredentials(InvalidCredentialsException exception) {
        return unauthorized("Bearer");
    }

    @ExceptionHandler(DomainValidationException.class)
    ResponseEntity<ProblemDetail> handleDomainValidation(DomainValidationException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
        problem.setProperty("field", exception.field());
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(PasswordHasherBusyException.class)
    ResponseEntity<ProblemDetail> handleHasherBusy(PasswordHasherBusyException exception) {
        log.warn("Login rejected: password hashing capacity exhausted");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .header(HttpHeaders.RETRY_AFTER, "1")
                .body(ProblemDetail.forStatus(HttpStatus.SERVICE_UNAVAILABLE));
    }

    @ExceptionHandler(RequestBodyTooLargeException.class)
    ResponseEntity<ProblemDetail> handleBodyTooLarge(RequestBodyTooLargeException exception) {
        log.debug("Request rejected: body exceeds the configured limit");
        return ResponseEntity.status(HttpStatus.CONTENT_TOO_LARGE)
                .body(ProblemDetail.forStatus(HttpStatus.CONTENT_TOO_LARGE));
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException exception,
                                                                  HttpHeaders headers, HttpStatusCode status,
                                                                  WebRequest request) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof RequestBodyTooLargeException) {
                return handleExceptionInternal(exception, null, headers, HttpStatus.CONTENT_TOO_LARGE, request);
            }
        }
        return super.handleHttpMessageNotReadable(exception, headers, status, request);
    }

    private static ResponseEntity<ProblemDetail> unauthorized(String challenge) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .header(HttpHeaders.WWW_AUTHENTICATE, challenge)
                .body(ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED));
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ProblemDetail> handleAccessDenied(AccessDeniedException exception) {
        log.debug("Access denied: {}", exception.getClass().getSimpleName());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ProblemDetail.forStatus(HttpStatus.FORBIDDEN));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> handleUnexpected(Exception exception) {
        log.error("Unhandled exception", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR));
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception exception, Object body, HttpHeaders headers,
                                                             HttpStatusCode statusCode, WebRequest request) {
        log.debug("Request rejected with {}: {}", statusCode.value(), exception.getClass().getSimpleName());
        return super.handleExceptionInternal(exception, ProblemDetail.forStatus(statusCode), headers, statusCode,
                request);
    }
}
