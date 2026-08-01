package com.repodna.util;

import com.repodna.exception.ValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.*;

public class DirectoryValidatorTest {

    @Test
    public void shouldThrowExceptionWhenPathIsNull() {
        assertThatThrownBy(() -> DirectoryValidator.validate(null))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("cannot be null");
    }

    @Test
    public void shouldThrowExceptionWhenPathDoesNotExist() {
        Path missing = Path.of("missing_directory_xyz");
        assertThatThrownBy(() -> DirectoryValidator.validate(missing))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("does not exist");
    }

    @Test
    public void shouldThrowExceptionWhenPathIsFile(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("regular_file.txt");
        Files.writeString(file, "hello");
        
        assertThatThrownBy(() -> DirectoryValidator.validate(file))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("not a directory");
    }

    @Test
    public void shouldPassWhenDirectoryIsValid(@TempDir Path tempDir) {
        assertThatCode(() -> DirectoryValidator.validate(tempDir))
            .doesNotThrowAnyException();
    }
}
