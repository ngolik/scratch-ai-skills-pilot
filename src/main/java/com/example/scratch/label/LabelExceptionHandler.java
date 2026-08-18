package com.example.scratch.label;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.example.scratch.greeting.FieldErrorDetail;
import com.example.scratch.greeting.ValidationErrorResponse;

/**
 * Label-specific error mapping, scoped to {@link LabelController} only, mirroring
 * {@code counter.CounterExceptionHandler} / {@code status.controller.StatusExceptionHandler}:
 * "label not found" is a concern no other controller raises, and the blank/too-long-after-trim
 * {@code value} rule cannot be expressed as a Bean Validation annotation (it depends on the
 * trimmed value), so neither belongs in the shared app-wide advice.
 */
@RestControllerAdvice(assignableTypes = LabelController.class)
public class LabelExceptionHandler {

    private static final String NOT_FOUND_ERROR = "not_found";
    private static final String VALIDATION_FAILED_ERROR = "validation_failed";
    private static final String NAME_FIELD = "name";
    private static final String VALUE_FIELD = "value";
    private static final String LABEL_NOT_FOUND_MESSAGE = "label does not exist";

    @ExceptionHandler(LabelNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ValidationErrorResponse handleLabelNotFound(LabelNotFoundException exception) {
        List<FieldErrorDetail> details = List.of(new FieldErrorDetail(NAME_FIELD, LABEL_NOT_FOUND_MESSAGE));
        return new ValidationErrorResponse(NOT_FOUND_ERROR, details);
    }

    @ExceptionHandler(LabelValueInvalidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ValidationErrorResponse handleLabelValueInvalid(LabelValueInvalidException exception) {
        List<FieldErrorDetail> details = List.of(new FieldErrorDetail(VALUE_FIELD, exception.getMessage()));
        return new ValidationErrorResponse(VALIDATION_FAILED_ERROR, details);
    }
}
