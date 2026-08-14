package com.example.scratch.notes.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.example.scratch.greeting.FieldErrorDetail;
import com.example.scratch.greeting.ValidationErrorResponse;
import com.example.scratch.notes.domain.InvalidNoteIdException;
import com.example.scratch.notes.domain.InvalidNoteTextException;
import com.example.scratch.notes.domain.NoteNotFoundException;

/**
 * Notes-specific error mapping, scoped to {@link NoteController} only. Notes' failures are
 * custom domain exceptions, not the Bean-Validation types the app-wide
 * {@code greeting.GreetingValidationExceptionHandler} handles, so they don't belong on that
 * shared advice — see {@code docs/architecture/memo-notes.md} §5.
 */
@RestControllerAdvice(assignableTypes = NoteController.class)
public class NoteExceptionHandler {

    private static final String VALIDATION_FAILED_ERROR = "validation_failed";
    private static final String NOT_FOUND_ERROR = "not_found";
    private static final String TEXT_FIELD = "text";
    private static final String ID_FIELD = "id";
    private static final String MALFORMED_ID_MESSAGE = "must be a valid UUID";
    private static final String NOTE_NOT_FOUND_MESSAGE = "note does not exist";

    @ExceptionHandler(InvalidNoteTextException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ValidationErrorResponse handleInvalidText(InvalidNoteTextException exception) {
        List<FieldErrorDetail> details = List.of(new FieldErrorDetail(TEXT_FIELD, exception.getMessage()));
        return new ValidationErrorResponse(VALIDATION_FAILED_ERROR, details);
    }

    @ExceptionHandler(InvalidNoteIdException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ValidationErrorResponse handleInvalidId(InvalidNoteIdException exception) {
        List<FieldErrorDetail> details = List.of(new FieldErrorDetail(ID_FIELD, MALFORMED_ID_MESSAGE));
        return new ValidationErrorResponse(VALIDATION_FAILED_ERROR, details);
    }

    @ExceptionHandler(NoteNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ValidationErrorResponse handleNotFound(NoteNotFoundException exception) {
        List<FieldErrorDetail> details = List.of(new FieldErrorDetail(ID_FIELD, NOTE_NOT_FOUND_MESSAGE));
        return new ValidationErrorResponse(NOT_FOUND_ERROR, details);
    }
}
