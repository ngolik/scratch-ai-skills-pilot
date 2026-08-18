package com.example.scratch.status.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.example.scratch.greeting.FieldErrorDetail;
import com.example.scratch.greeting.ValidationErrorResponse;
import com.example.scratch.status.service.StatusMessageInvalidException;
import com.example.scratch.status.service.StatusNotFoundException;

/**
 * Status-specific error mapping, scoped to {@link StatusController} only, mirroring
 * {@code toggle.controller.ToggleExceptionHandler}: "status not found" is a concern no other
 * controller raises, and the blank/too-long-after-trim {@code message} rule cannot be expressed
 * as a Bean Validation annotation (it depends on the trimmed value), so neither belongs in the
 * shared app-wide advice.
 */
@RestControllerAdvice(assignableTypes = StatusController.class)
public class StatusExceptionHandler {

    private static final String NOT_FOUND_ERROR = "not_found";
    private static final String VALIDATION_FAILED_ERROR = "validation_failed";
    private static final String NAME_FIELD = "name";
    private static final String MESSAGE_FIELD = "message";
    private static final String STATUS_NOT_FOUND_MESSAGE = "status does not exist";

    @ExceptionHandler(StatusNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ValidationErrorResponse handleStatusNotFound(StatusNotFoundException exception) {
        List<FieldErrorDetail> details = List.of(new FieldErrorDetail(NAME_FIELD, STATUS_NOT_FOUND_MESSAGE));
        return new ValidationErrorResponse(NOT_FOUND_ERROR, details);
    }

    @ExceptionHandler(StatusMessageInvalidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ValidationErrorResponse handleStatusMessageInvalid(StatusMessageInvalidException exception) {
        List<FieldErrorDetail> details = List.of(new FieldErrorDetail(MESSAGE_FIELD, exception.getMessage()));
        return new ValidationErrorResponse(VALIDATION_FAILED_ERROR, details);
    }
}
