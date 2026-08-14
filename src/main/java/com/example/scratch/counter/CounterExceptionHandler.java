package com.example.scratch.counter;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.example.scratch.greeting.FieldErrorDetail;
import com.example.scratch.greeting.ValidationErrorResponse;

/**
 * Counter-specific error mapping, scoped to {@link CounterController} only. Unlike the app-wide
 * {@code greeting.GreetingValidationExceptionHandler}, "counter not found" is a concern no other
 * controller in this application raises, so it does not belong in the shared advice.
 */
@RestControllerAdvice(assignableTypes = CounterController.class)
public class CounterExceptionHandler {

    private static final String NOT_FOUND_ERROR = "not_found";
    private static final String NAME_FIELD = "name";
    private static final String COUNTER_NOT_FOUND_MESSAGE = "counter does not exist";

    @ExceptionHandler(CounterNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ValidationErrorResponse handleCounterNotFound(CounterNotFoundException exception) {
        List<FieldErrorDetail> details = List.of(new FieldErrorDetail(NAME_FIELD, COUNTER_NOT_FOUND_MESSAGE));
        return new ValidationErrorResponse(NOT_FOUND_ERROR, details);
    }
}
