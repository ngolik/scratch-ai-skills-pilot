package com.example.scratch.farewell;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.example.scratch.validation.NameValidationConstants;

public record FarewellRequest(

        @NotBlank(message = "must not be blank")
        @Size(max = NameValidationConstants.MAX_NAME_LENGTH, message = "must be at most 40 characters")
        @Pattern(regexp = NameValidationConstants.NAME_PATTERN, message = "must contain only letters, spaces, hyphens, and apostrophes")
        String name,

        @Pattern(regexp = NameValidationConstants.LOCALE_PATTERN, message = "must be one of: en, es, de")
        String locale) {
}
