package com.example.scratch.notes.domain;

public record NoteText(String value) {

    static final int MAX_LENGTH = 200;
    static final String BLANK_MESSAGE = "must not be blank";
    static final String TOO_LONG_MESSAGE = "must be at most 200 characters";

    public NoteText {
        if (value == null) {
            throw new InvalidNoteTextException(BLANK_MESSAGE);
        }
        value = value.trim();
        if (value.isEmpty()) {
            throw new InvalidNoteTextException(BLANK_MESSAGE);
        }
        if (value.length() > MAX_LENGTH) {
            throw new InvalidNoteTextException(TOO_LONG_MESSAGE);
        }
    }
}
