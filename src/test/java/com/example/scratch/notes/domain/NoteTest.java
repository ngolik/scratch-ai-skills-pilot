package com.example.scratch.notes.domain;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NoteTest {

    @Test
    void create_WhenCalled_GeneratesFreshIdWithGivenTextAndTimestamp() {
        NoteText text = new NoteText("Ship the plugin test");
        Instant createdAt = Instant.parse("2026-08-14T10:00:00Z");

        Note note = Note.create(text, createdAt);

        assertThat(note.id()).isNotNull();
        assertThat(note.text()).isEqualTo(text);
        assertThat(note.createdAt()).isEqualTo(createdAt);
    }

    @Test
    void create_WhenCalledTwice_GeneratesDistinctIds() {
        NoteText text = new NoteText("Ship the plugin test");
        Instant createdAt = Instant.now();

        Note first = Note.create(text, createdAt);
        Note second = Note.create(text, createdAt);

        assertThat(first.id()).isNotEqualTo(second.id());
    }
}
