package com.example.scratch.notes.domain;

import java.util.Optional;

public interface NoteRepository {

    void save(Note note);

    Optional<Note> findById(NoteId id);
}
