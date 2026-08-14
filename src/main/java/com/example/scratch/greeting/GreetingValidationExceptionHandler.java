package com.example.scratch.greeting;

import java.util.List;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;

import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * App-wide validation-error mapping. This advice has no {@code basePackageClasses} /
 * {@code assignableTypes} restriction, so it applies to every {@code @Valid}-annotated
 * controller in the application — currently {@code greeting.GreetingController},
 * {@code farewell.FarewellController}, and {@code counter.CounterController} all rely on it for
 * their {@code 400} contract. Keep it package-agnostic; do not scope it to {@code greeting} only.
 */
@RestControllerAdvice
public class GreetingValidationExceptionHandler {

    private static final String VALIDATION_FAILED_ERROR = "validation_failed";
    private static final String MALFORMED_BODY_FIELD = "body";
    private static final String MALFORMED_BODY_MESSAGE = "must be valid JSON matching the expected request shape";
    private static final String LOCALE_FIELD = "locale";
    private static final String UNSUPPORTED_LOCALE_MESSAGE = "must be one of: en, es, de";

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

    /**
     * Defense-in-depth for {@code GreetingLocale.resolve} / {@code FarewellLocale.resolve}:
     * both throw {@link IllegalArgumentException} for a code outside their enum, which is
     * unreachable today only because each request DTO's {@code @Pattern} is hand-kept in sync
     * with its locale enum. If that sync is ever broken, this turns the failure back into the
     * standard {@code 400} shape instead of an unhandled {@code 500}.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ValidationErrorResponse handleUnsupportedLocale(IllegalArgumentException exception) {
        List<FieldErrorDetail> details = List.of(new FieldErrorDetail(LOCALE_FIELD, UNSUPPORTED_LOCALE_MESSAGE));
        return new ValidationErrorResponse(VALIDATION_FAILED_ERROR, details);
    }

    /**
     * Covers {@code @PathVariable}/{@code @RequestParam} constraint violations (e.g.
     * {@code counter.CounterController}'s {@code {name}}), which Spring reports as a
     * {@link ConstraintViolationException} rather than {@link MethodArgumentNotValidException}.
     * The violation's property path is method-qualified (e.g. {@code incrementCounter.name}); only
     * the leaf segment is surfaced as {@code field} to match the {@code @RequestBody} handlers above.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ValidationErrorResponse handlePathVariableViolation(ConstraintViolationException exception) {
        List<FieldErrorDetail> details = exception.getConstraintViolations().stream()
                .map(violation -> new FieldErrorDetail(leafPropertyName(violation.getPropertyPath()), violation.getMessage()))
                .toList();
        return new ValidationErrorResponse(VALIDATION_FAILED_ERROR, details);
    }

    private static String leafPropertyName(Path propertyPath) {
        String fullPath = propertyPath.toString();
        int lastDot = fullPath.lastIndexOf('.');
        return lastDot >= 0 ? fullPath.substring(lastDot + 1) : fullPath;
    }
}
