package com.example.scratch.toggle.dto;

import jakarta.validation.constraints.NotNull;

public record SetToggleRequest(

        @NotNull(message = "must be a boolean")
        Boolean enabled) {
}
