package com.example.scratch.notes.application;

import java.time.Instant;

import org.springframework.stereotype.Service;

import com.example.scratch.notes.domain.Note;
import com.example.scratch.notes.domain.NoteId;
import com.example.scratch.notes.domain.NoteNotFoundException;
import com.example.scratch.notes.domain.NoteRepository;
import com.example.scratch.notes.domain.NoteText;

@Service
public class NoteApplicationService {

    private final NoteRepository noteRepository;

    public NoteApplicationService(NoteRepository noteRepository) {
        this.noteRepository = noteRepository;
    }

    public Note createNote(String rawText) {
        Note note = Note.create(new NoteText(rawText), Instant.now());
        noteRepository.save(note);
        return note;
    }

    public Note getNote(String rawId) {
        NoteId id = NoteId.fromString(rawId);
        return noteRepository.findById(id).orElseThrow(() -> new NoteNotFoundException(id));
    }
}
