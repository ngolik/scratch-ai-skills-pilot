package com.example.scratch.status;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Enforces the brief's dependency-direction rule (controller -> service -> repository) at the
 * source-text level, since this repo has no ArchUnit dependency to express it as bytecode rules.
 */
class StatusLayeringTest {

    private static final Path CONTROLLER_SOURCE =
            Path.of("src/main/java/com/example/scratch/status/controller/StatusController.java");
    private static final Path REPOSITORY_SOURCE =
            Path.of("src/main/java/com/example/scratch/status/repository/InMemoryStatusRepository.java");

    @Test
    void controllerSource_DoesNotImportRepositoryPackage() throws IOException {
        String source = Files.readString(CONTROLLER_SOURCE);

        assertThat(source).doesNotContain("import com.example.scratch.status.repository");
    }

    @Test
    void repositorySource_DoesNotImportDtoPackage() throws IOException {
        String source = Files.readString(REPOSITORY_SOURCE);

        assertThat(source).doesNotContain("import com.example.scratch.status.dto");
    }
}
