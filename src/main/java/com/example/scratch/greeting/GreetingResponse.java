package com.example.scratch.greeting;

import java.time.Instant;

public record GreetingResponse(String message, String name, String locale, Instant generatedAt) {
}
