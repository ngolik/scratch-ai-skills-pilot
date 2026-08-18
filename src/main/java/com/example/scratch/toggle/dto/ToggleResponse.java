package com.example.scratch.toggle.dto;

import java.time.Instant;

public record ToggleResponse(String name, boolean enabled, Instant updatedAt) {
}
