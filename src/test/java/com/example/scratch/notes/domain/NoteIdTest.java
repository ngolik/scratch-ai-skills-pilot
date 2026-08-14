package com.example.scratch.notes.domain;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NoteIdTest {

    @Test
    void fromString_WhenValueIsValidUuid_ParsesSuccessfully() {
        UUID uuid = UUID.randomUUID();

        NoteId id = NoteId.fromString(uuid.toString());

        assertThat(id.value()).isEqualTo(uuid);
    }

    @Test
    void fromString_WhenValueIsMalformed_ThrowsInvalidNoteIdException() {
        assertThatThrownBy(() -> NoteId.fromString("not-a-uuid"))
                .isInstanceOf(InvalidNoteIdException.class)
                .hasNoCause();
    }

    @Test
    void fromString_WhenValueIsNull_ThrowsInvalidNoteIdException() {
        assertThatThrownBy(() -> NoteId.fromString(null))
                .isInstanceOf(InvalidNoteIdException.class);
    }

    @Test
    void newId_WhenCalledTwice_ProducesDistinctIds() {
        NoteId first = NoteId.newId();
        NoteId second = NoteId.newId();

        assertThat(first).isNotEqualTo(second);
    }
}
