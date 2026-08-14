package com.example.scratch.notes.web;

import java.time.Instant;

public record NoteResponse(String id, String text, Instant createdAt) {
}
