package com.example.scratch.toggle.repository;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.scratch.toggle.entity.Toggle;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryToggleRepositoryTest {

    private InMemoryToggleRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryToggleRepository();
    }

    @Test
    void findByName_AfterSave_ReturnsSameToggle() {
        Toggle saved = repository.save(new Toggle("dark-mode", true, Instant.now()));

        Optional<Toggle> found = repository.findByName("dark-mode");

        assertThat(found).contains(saved);
    }

    @Test
    void findByName_WhenNameUnknown_ReturnsEmpty() {
        Optional<Toggle> found = repository.findByName("missing");

        assertThat(found).isEmpty();
    }

    @Test
    void save_WhenNameAlreadyExists_OverwritesToggle() {
        repository.save(new Toggle("dark-mode", true, Instant.now()));

        Toggle updated = repository.save(new Toggle("dark-mode", false, Instant.now()));

        assertThat(repository.findByName("dark-mode")).contains(updated);
        assertThat(repository.findByName("dark-mode").orElseThrow().enabled()).isFalse();
    }
}
