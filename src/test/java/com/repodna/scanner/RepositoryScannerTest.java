package com.repodna.scanner;

import com.repodna.scanner.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.*;

public class RepositoryScannerTest {

    @Test
    public void shouldScanDummyJavaSpringGradleRepo(@TempDir Path tempDir) throws IOException {
        Files.createDirectories(tempDir.resolve("src/main/java/com/example"));
        Files.createDirectories(tempDir.resolve("build"));
        Files.createDirectories(tempDir.resolve("node_modules"));
        
        Files.writeString(tempDir.resolve("build.gradle.kts"), "plugins { id(\"org.springframework.boot\") } \n spring-boot-starter-web");
        Files.writeString(tempDir.resolve("src/main/java/com/example/App.java"), "@SpringBootApplication class App {}");
        Files.writeString(tempDir.resolve("README.md"), "# Mock Project");
        Files.writeString(tempDir.resolve(".gitignore"), "build/");
        Files.writeString(tempDir.resolve("Dockerfile"), "FROM openjdk");

        ScannerResult result = RepositoryScanner.scan(tempDir);

        assertThat(result.stats().fileCount()).isEqualTo(5);
        assertThat(result.stats().directoryCount()).isEqualTo(5); // src, src/main, src/main/java, src/main/java/com, src/main/java/com/example

        assertThat(result.buildSystem().name()).isEqualTo("Gradle Kotlin DSL");
        assertThat(result.buildSystem().packageManager()).isEqualTo("Gradle");

        assertThat(result.primaryLanguage().name()).isEqualTo("Java");

        assertThat(result.frameworks()).hasSize(1);
        assertThat(result.frameworks().get(0).name()).isEqualTo("Spring Boot");

        assertThat(result.documentation().presentDocs()).contains("README.md");

        assertThat(result.configs()).contains("Dockerfile", ".gitignore");
    }

    @Test
    public void shouldIgnoreSpecifiedDirectories(@TempDir Path tempDir) throws IOException {
        Files.createDirectories(tempDir.resolve("node_modules/library"));
        Files.writeString(tempDir.resolve("node_modules/library/package.json"), "{}");
        Files.writeString(tempDir.resolve("README.md"), "# Mock Project");

        ScannerResult result = RepositoryScanner.scan(tempDir);

        assertThat(result.stats().fileCount()).isEqualTo(1);
        assertThat(result.stats().directoryCount()).isEqualTo(0);
    }
}
