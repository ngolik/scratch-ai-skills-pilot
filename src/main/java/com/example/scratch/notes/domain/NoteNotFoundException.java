package com.example.scratch.notes.domain;

public class NoteNotFoundException extends RuntimeException {

    public NoteNotFoundException(NoteId id) {
        super("Note not found: " + id.value());
    }
}
