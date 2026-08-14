package com.example.scratch.notes.domain;

public class InvalidNoteTextException extends RuntimeException {

    public InvalidNoteTextException(String message) {
        super(message);
    }
}
