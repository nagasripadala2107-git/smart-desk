package com.smartdesk.exception;

import com.smartdesk.dto.common.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                "RESOURCE_NOT_FOUND",
                ex.getMessage(),
                request.getRequestURI()
        );
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(BadRequestException ex, HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "BAD_REQUEST",
                ex.getMessage(),
                request.getRequestURI()
        );
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(UnauthorizedException ex, HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.UNAUTHORIZED.value(),
                "UNAUTHORIZED",
                ex.getMessage(),
                request.getRequestURI()
        );
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex, HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.UNAUTHORIZED.value(),
                "INVALID_CREDENTIALS",
                "Invalid email or password",
                request.getRequestURI()
        );
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler({ForbiddenException.class, AccessDeniedException.class})
    public ResponseEntity<ErrorResponse> handleForbidden(Exception ex, HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.FORBIDDEN.value(),
                "FORBIDDEN",
                ex.getMessage() != null ? ex.getMessage() : "Access denied for this resource",
                request.getRequestURI()
        );
        return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(ConflictException ex, HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.CONFLICT.value(),
                "CONFLICT",
                ex.getMessage(),
                request.getRequestURI()
        );
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> validationErrors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            validationErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "VALIDATION_FAILED",
                "Input validation failed",
                request.getRequestURI(),
                validationErrors
        );
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(
            org.springframework.http.converter.HttpMessageNotReadableException ex,
            HttpServletRequest request
    ) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "MALFORMED_REQUEST",
                "Malformed JSON request payload or invalid parameter value",
                request.getRequestURI()
        );
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatch(
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException ex,
            HttpServletRequest request
    ) {
        String paramName = ex.getName() != null ? ex.getName() : "parameter";
        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "INVALID_PARAMETER",
                "Invalid parameter format for '" + paramName + "'",
                request.getRequestURI()
        );
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(
            org.springframework.web.HttpRequestMethodNotSupportedException ex,
            HttpServletRequest request
    ) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.METHOD_NOT_ALLOWED.value(),
                "METHOD_NOT_ALLOWED",
                "HTTP method " + ex.getMethod() + " is not supported for this endpoint",
                request.getRequestURI()
        );
        return new ResponseEntity<>(error, HttpStatus.METHOD_NOT_ALLOWED);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
        log.error("Unhandled internal server error on path {}: ", request.getRequestURI(), ex);
        ErrorResponse error = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "INTERNAL_SERVER_ERROR",
                "An unexpected internal error occurred. Please try again later.",
                request.getRequestURI()
        );
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
