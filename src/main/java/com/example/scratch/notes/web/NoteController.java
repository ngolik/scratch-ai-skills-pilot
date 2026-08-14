package com.example.scratch.notes.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.scratch.notes.application.NoteApplicationService;
import com.example.scratch.notes.domain.Note;

/**
 * No {@code @Valid}/{@code @Pattern} here — {@code text} and {@code id} invariants are enforced
 * by the domain layer ({@code NoteText}, {@code NoteId}); this controller only maps HTTP in/out
 * and lets {@link NoteExceptionHandler} translate the resulting domain exceptions.
 */
@RestController
public class NoteController {

    static final String NOTES_PATH = "/api/v1/notes";
    static final String NOTE_PATH = "/api/v1/notes/{id}";

    private final NoteApplicationService noteApplicationService;

    public NoteController(NoteApplicationService noteApplicationService) {
        this.noteApplicationService = noteApplicationService;
    }

    @PostMapping(path = NOTES_PATH, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public NoteResponse createNote(@RequestBody CreateNoteRequest request) {
        return toResponse(noteApplicationService.createNote(request.text()));
    }

    @GetMapping(path = NOTE_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
    public NoteResponse getNote(@PathVariable String id) {
        return toResponse(noteApplicationService.getNote(id));
    }

    private static NoteResponse toResponse(Note note) {
        return new NoteResponse(note.id().value().toString(), note.text().value(), note.createdAt());
    }
}
