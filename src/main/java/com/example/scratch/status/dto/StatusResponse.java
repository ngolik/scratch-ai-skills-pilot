package com.example.scratch.status.dto;

import java.time.Instant;

public record StatusResponse(String name, String message, Instant updatedAt) {
}
