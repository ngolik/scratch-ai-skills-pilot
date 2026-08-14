package com.example.scratch.notes.application;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.scratch.notes.domain.InvalidNoteIdException;
import com.example.scratch.notes.domain.InvalidNoteTextException;
import com.example.scratch.notes.domain.Note;
import com.example.scratch.notes.domain.NoteId;
import com.example.scratch.notes.domain.NoteNotFoundException;
import com.example.scratch.notes.domain.NoteRepository;
import com.example.scratch.notes.domain.NoteText;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class NoteApplicationServiceTest {

    private NoteRepository noteRepository;
    private NoteApplicationService noteApplicationService;

    @BeforeEach
    void setUp() {
        noteRepository = mock(NoteRepository.class);
        noteApplicationService = new NoteApplicationService(noteRepository);
    }

    @Test
    void createNote_WhenTextIsValid_SavesAndReturnsTrimmedNote() {
        Note note = noteApplicationService.createNote("  Ship the plugin test  ");

        assertThat(note.text().value()).isEqualTo("Ship the plugin test");
        verify(noteRepository).save(note);
    }

    @Test
    void createNote_WhenTextIsInvalid_ThrowsAndNeverCallsRepository() {
        assertThatThrownBy(() -> noteApplicationService.createNote("   "))
                .isInstanceOf(InvalidNoteTextException.class);

        verifyNoInteractions(noteRepository);
    }

    @Test
    void getNote_WhenIdIsMalformed_ThrowsAndNeverCallsRepository() {
        assertThatThrownBy(() -> noteApplicationService.getNote("not-a-uuid"))
                .isInstanceOf(InvalidNoteIdException.class);

        verifyNoInteractions(noteRepository);
    }

    @Test
    void getNote_WhenIdIsWellFormedButUnknown_ThrowsNoteNotFoundException() {
        NoteId id = NoteId.newId();
        when(noteRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> noteApplicationService.getNote(id.value().toString()))
                .isInstanceOf(NoteNotFoundException.class);
    }

    @Test
    void getNote_WhenIdIsKnown_ReturnsStoredNote() {
        Note note = Note.create(new NoteText("Ship the plugin test"), Instant.now());
        when(noteRepository.findById(note.id())).thenReturn(Optional.of(note));

        Note found = noteApplicationService.getNote(note.id().value().toString());

        assertThat(found).isEqualTo(note);
        verify(noteRepository, never()).save(any());
    }
}
