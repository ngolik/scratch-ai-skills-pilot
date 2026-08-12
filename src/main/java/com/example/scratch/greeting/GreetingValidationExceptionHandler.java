package com.example.scratch.greeting;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GreetingValidationExceptionHandler {

    private static final String VALIDATION_FAILED_ERROR = "validation_failed";
    private static final String MALFORMED_BODY_FIELD = "body";
    private static final String MALFORMED_BODY_MESSAGE = "must be valid JSON matching the expected request shape";

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ValidationErrorResponse handleValidationException(MethodArgumentNotValidException exception) {
        List<FieldErrorDetail> details = exception.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> new FieldErrorDetail(fieldError.getField(), fieldError.getDefaultMessage()))
                .toList();
        return new ValidationErrorResponse(VALIDATION_FAILED_ERROR, details);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ValidationErrorResponse handleMalformedBody(HttpMessageNotReadableException exception) {
        List<FieldErrorDetail> details = List.of(new FieldErrorDetail(MALFORMED_BODY_FIELD, MALFORMED_BODY_MESSAGE));
        return new ValidationErrorResponse(VALIDATION_FAILED_ERROR, details);
    }
}
