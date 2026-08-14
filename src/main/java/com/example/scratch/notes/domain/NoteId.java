package com.example.scratch.notes.domain;

import java.util.UUID;

public record NoteId(UUID value) {

    public static NoteId newId() {
        return new NoteId(UUID.randomUUID());
    }

    /**
     * Deliberately does not chain the caught exception as {@code InvalidNoteIdException}'s cause:
     * {@code greeting.GreetingValidationExceptionHandler} has an app-wide
     * {@code @ExceptionHandler(IllegalArgumentException.class)}, and Spring's exception-handler
     * resolution falls back to an exception's cause chain when no handler matches its own type —
     * chaining an {@code IllegalArgumentException} here would misroute this to that unrelated,
     * locale-specific handler instead of {@code notes.web.NoteExceptionHandler}.
     */
    public static NoteId fromString(String raw) {
        if (raw == null) {
            throw new InvalidNoteIdException(null);
        }
        try {
            return new NoteId(UUID.fromString(raw));
        } catch (IllegalArgumentException e) {
            throw new InvalidNoteIdException(raw);
        }
    }
}
