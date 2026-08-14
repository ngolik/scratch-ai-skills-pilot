package com.example.scratch.notes.domain;

import java.time.Instant;

public record Note(NoteId id, NoteText text, Instant createdAt) {

    public static Note create(NoteText text, Instant createdAt) {
        return new Note(NoteId.newId(), text, createdAt);
    }
}
