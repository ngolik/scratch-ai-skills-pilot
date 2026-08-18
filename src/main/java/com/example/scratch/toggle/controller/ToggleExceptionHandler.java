package com.example.scratch.toggle.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.example.scratch.greeting.FieldErrorDetail;
import com.example.scratch.greeting.ValidationErrorResponse;
import com.example.scratch.toggle.service.ToggleNotFoundException;

/**
 * Toggle-specific error mapping, scoped to {@link ToggleController} only, mirroring
 * {@code counter.CounterExceptionHandler}: "toggle not found" is a concern no other controller
 * raises, so it does not belong in the shared app-wide advice.
 */
@RestControllerAdvice(assignableTypes = ToggleController.class)
public class ToggleExceptionHandler {

    private static final String NOT_FOUND_ERROR = "not_found";
    private static final String NAME_FIELD = "name";
    private static final String TOGGLE_NOT_FOUND_MESSAGE = "toggle does not exist";

    @ExceptionHandler(ToggleNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ValidationErrorResponse handleToggleNotFound(ToggleNotFoundException exception) {
        List<FieldErrorDetail> details = List.of(new FieldErrorDetail(NAME_FIELD, TOGGLE_NOT_FOUND_MESSAGE));
        return new ValidationErrorResponse(NOT_FOUND_ERROR, details);
    }
}
