package com.example.scratch.status.entity;

import java.time.Instant;

public record Status(String name, String message, Instant updatedAt) {
}
