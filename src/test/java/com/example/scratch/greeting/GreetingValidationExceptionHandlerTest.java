package com.example.scratch.greeting;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

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
}
