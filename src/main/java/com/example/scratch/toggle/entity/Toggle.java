package com.example.scratch.toggle.entity;

import java.time.Instant;

public record Toggle(String name, boolean enabled, Instant updatedAt) {
}
