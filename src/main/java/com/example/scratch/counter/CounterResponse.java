package com.example.scratch.counter;

import java.time.Instant;

public record CounterResponse(String name, long value, Instant updatedAt) {
}
