package com.example.scratch.farewell;

import java.time.Instant;

public record FarewellResponse(String message, String name, String locale, Instant generatedAt) {
}
