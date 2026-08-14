package com.example.scratch.notes.infrastructure;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.example.scratch.notes.domain.Note;
import com.example.scratch.notes.domain.NoteId;
import com.example.scratch.notes.domain.NoteText;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryNoteRepositoryTest {

    private final InMemoryNoteRepository repository = new InMemoryNoteRepository();

    @Test
    void findById_AfterSave_ReturnsTheSameNote() {
        Note note = Note.create(new NoteText("Ship the plugin test"), Instant.now());

        repository.save(note);
        Optional<Note> found = repository.findById(note.id());

        assertThat(found).contains(note);
    }

    @Test
    void findById_WhenNeverSaved_ReturnsEmpty() {
        Optional<Note> found = repository.findById(NoteId.newId());

        assertThat(found).isEmpty();
    }
}
