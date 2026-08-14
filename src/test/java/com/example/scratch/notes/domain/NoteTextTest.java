package com.example.scratch.notes.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NoteTextTest {

    @Test
    void constructor_WhenTextHasSurroundingWhitespace_TrimsValue() {
        NoteText text = new NoteText("  Ship the plugin test  ");

        assertThat(text.value()).isEqualTo("Ship the plugin test");
    }

    @Test
    void constructor_WhenTextIsBlank_ThrowsInvalidNoteTextException() {
        assertThatThrownBy(() -> new NoteText("   "))
                .isInstanceOf(InvalidNoteTextException.class)
                .hasMessage("must not be blank");
    }

    @Test
    void constructor_WhenTextIsNull_ThrowsInvalidNoteTextException() {
        assertThatThrownBy(() -> new NoteText(null))
                .isInstanceOf(InvalidNoteTextException.class)
                .hasMessage("must not be blank");
    }

    @Test
    void constructor_WhenTrimmedTextIsExactly200Characters_Passes() {
        String text = "a".repeat(200);

        NoteText noteText = new NoteText(text);

        assertThat(noteText.value()).hasSize(200);
    }

    @Test
    void constructor_WhenTrimmedTextIsOver200Characters_ThrowsInvalidNoteTextException() {
        String text = "a".repeat(201);

        assertThatThrownBy(() -> new NoteText(text))
                .isInstanceOf(InvalidNoteTextException.class)
                .hasMessage("must be at most 200 characters");
    }
}
