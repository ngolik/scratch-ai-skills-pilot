package com.example.scratch.label;

import java.time.Instant;

public record LabelResponse(String name, String value, Instant updatedAt) {
}
