package com.example.scratch.notes.infrastructure;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.stereotype.Repository;

import com.example.scratch.notes.domain.Note;
import com.example.scratch.notes.domain.NoteId;
import com.example.scratch.notes.domain.NoteRepository;

@Repository
public class InMemoryNoteRepository implements NoteRepository {

    private final ConcurrentMap<NoteId, Note> notes = new ConcurrentHashMap<>();

    @Override
    public void save(Note note) {
        notes.put(note.id(), note);
    }

    @Override
    public Optional<Note> findById(NoteId id) {
        return Optional.ofNullable(notes.get(id));
    }
}
