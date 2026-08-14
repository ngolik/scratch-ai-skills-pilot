package com.example.scratch.notes.domain;

public class InvalidNoteIdException extends RuntimeException {

    public InvalidNoteIdException(String rawId) {
        super("Invalid note id: " + rawId);
    }
}
