package com.example.scratch.label;

import jakarta.validation.constraints.NotNull;

public record LabelRequest(

        @NotNull(message = "must not be blank")
        String value) {
}
