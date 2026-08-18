package com.example.scratch.status.dto;

import jakarta.validation.constraints.NotNull;

public record SetStatusRequest(

        @NotNull(message = "must not be blank")
        String message) {
}
