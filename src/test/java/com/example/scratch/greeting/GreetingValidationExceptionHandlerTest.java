package com.example.scratch.greeting;

import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GreetingValidationExceptionHandlerTest {

    private final GreetingValidationExceptionHandler handler = new GreetingValidationExceptionHandler();

    @Test
    void handleUnsupportedLocale_WhenLocaleResolutionThrows_ReturnsValidationFailedShape() {
        ValidationErrorResponse response =
                handler.handleUnsupportedLocale(new IllegalArgumentException("Unsupported locale: fr"));

        assertThat(response.error()).isEqualTo("validation_failed");
        assertThat(response.details()).hasSize(1);
        assertThat(response.details().get(0).field()).isEqualTo("locale");
        assertThat(response.details().get(0).message()).isEqualTo("must be one of: en, es, de");
    }

    @Test
    void handlePathVariableViolation_WhenPropertyPathHasMultipleSegments_ReturnsLeafFieldName() {
        ConstraintViolationException exception =
                constraintViolationException("incrementCounter.name", "must not be blank");

        ValidationErrorResponse response = handler.handlePathVariableViolation(exception);

        assertThat(response.error()).isEqualTo("validation_failed");
        assertThat(response.details()).hasSize(1);
        assertThat(response.details().get(0).field()).isEqualTo("name");
        assertThat(response.details().get(0).message()).isEqualTo("must not be blank");
    }

    @Test
    void handlePathVariableViolation_WhenPropertyPathHasNoDot_ReturnsFullPathAsFieldName() {
        ConstraintViolationException exception = constraintViolationException("name", "must not be blank");

        ValidationErrorResponse response = handler.handlePathVariableViolation(exception);

        assertThat(response.details().get(0).field()).isEqualTo("name");
    }

    private static ConstraintViolationException constraintViolationException(String propertyPath, String message) {
        Path path = mock(Path.class);
        when(path.toString()).thenReturn(propertyPath);

        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn(message);

        return new ConstraintViolationException(Set.of(violation));
    }
}
