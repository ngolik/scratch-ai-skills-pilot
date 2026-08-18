package com.example.scratch.status.repository;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.scratch.status.entity.Status;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryStatusRepositoryTest {

    private InMemoryStatusRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryStatusRepository();
    }

    @Test
    void findByName_AfterSave_ReturnsSameStatus() {
        Status saved = repository.save(new Status("build-bot", "away", Instant.now()));

        Optional<Status> found = repository.findByName("build-bot");

        assertThat(found).contains(saved);
    }

    @Test
    void findByName_WhenNameUnknown_ReturnsEmpty() {
        Optional<Status> found = repository.findByName("missing");

        assertThat(found).isEmpty();
    }

    @Test
    void save_WhenNameAlreadyExists_OverwritesStatus() {
        repository.save(new Status("build-bot", "away", Instant.now()));

        Status updated = repository.save(new Status("build-bot", "back", Instant.now()));

        assertThat(repository.findByName("build-bot")).contains(updated);
        assertThat(repository.findByName("build-bot").orElseThrow().message()).isEqualTo("back");
    }
}
