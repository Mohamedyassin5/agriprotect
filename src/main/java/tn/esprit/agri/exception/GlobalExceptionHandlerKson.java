package tn.esprit.agri.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandlerKson {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(NotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage(), List.of(), request.getRequestURI());
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ApiErrorResponse> handleBusiness(BusinessRuleException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "BUSINESS_RULE_VIOLATION", ex.getMessage(), List.of(), request.getRequestURI());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<String> details = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(this::formatFieldError)
                .toList();
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Invalid request payload", details, request.getRequestURI());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraint(ConstraintViolationException ex, HttpServletRequest request) {
        List<String> details = ex.getConstraintViolations().stream().map(v -> v.getMessage()).toList();
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Invalid request parameter", details, request.getRequestURI());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", ex.getMessage(), List.of(), request.getRequestURI());
    }

    private ResponseEntity<ApiErrorResponse> build(HttpStatus status, String code, String message, List<String> details, String path) {
        return ResponseEntity.status(status).body(ApiErrorResponse.builder()
                .code(code)
                .message(message)
                .details(details)
                .timestamp(LocalDateTime.now())
                .path(path)
                .build());
    }

    private String formatFieldError(FieldError e) {
        return e.getField() + ": " + e.getDefaultMessage();
    }
}
