package com.example.scratch.greeting;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record GreetingRequest(

        @NotBlank(message = "must not be blank")
        @Size(max = GreetingRequest.MAX_NAME_LENGTH, message = "must be at most 40 characters")
        @Pattern(regexp = GreetingRequest.NAME_PATTERN, message = "must contain only letters, spaces, hyphens, and apostrophes")
        String name,

        @Pattern(regexp = GreetingRequest.LOCALE_PATTERN, message = "must be one of: en, es, de")
        String locale) {

    static final int MAX_NAME_LENGTH = 40;
    static final String NAME_PATTERN = "^[\\p{L}\\s'-]*$";
    static final String LOCALE_PATTERN = "^(en|es|de)$";
}
