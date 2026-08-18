package com.example.scratch.notes;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Enforces the architecture's one required layering rule (docs/architecture/memo-notes.md §5):
 * no compile-time import from {@code notes.web} to {@code notes.infrastructure}. A source-text
 * scan, not ArchUnit — see the architecture doc for why.
 */
class NotesLayeringTest {

    private static final String WEB_PACKAGE_DIR = "src/main/java/com/example/scratch/notes/web";
    private static final String FORBIDDEN_IMPORT = "com.example.scratch.notes.infrastructure";

    @Test
    void webPackage_DoesNotImportInfrastructurePackage() throws IOException {
        Path webDir = Path.of(WEB_PACKAGE_DIR);
        assertThat(webDir).isDirectory();

        List<Path> javaFiles;
        try (Stream<Path> files = Files.walk(webDir)) {
            javaFiles = files.filter(path -> path.toString().endsWith(".java")).toList();
        }
        assertThat(javaFiles).isNotEmpty();

        for (Path file : javaFiles) {
            String content = readFile(file);
            assertThat(content)
                    .as("%s must not import %s", file, FORBIDDEN_IMPORT)
                    .doesNotContain(FORBIDDEN_IMPORT);
        }
    }

    private static String readFile(Path file) {
        try {
            return Files.readString(file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
