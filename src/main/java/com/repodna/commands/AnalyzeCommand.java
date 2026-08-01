package com.repodna.commands;

import com.repodna.exception.CommandException;
import com.repodna.util.DirectoryValidator;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Command to validate target directory structure and discover project layout.
 */
@Command(
    name = "analyze",
    description = "Validate target directory and detect project layout metadata",
    mixinStandardHelpOptions = true
)
public class AnalyzeCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "The target directory to analyze", defaultValue = ".")
    private Path dir = Path.of(".");

    @Override
    public Integer call() {
        Path targetDir = dir.toAbsolutePath().normalize();
        DirectoryValidator.validate(targetDir);

        boolean isGit = Files.exists(targetDir.resolve(".git"));
        boolean isGradle = Files.exists(targetDir.resolve("build.gradle")) || Files.exists(targetDir.resolve("build.gradle.kts"));
        boolean isMaven = Files.exists(targetDir.resolve("pom.xml"));

        String buildSystem = "None";
        if (isGradle) buildSystem = "Gradle";
        else if (isMaven) buildSystem = "Maven";

        AtomicInteger fileCount = new AtomicInteger(0);
        AtomicInteger dirCount = new AtomicInteger(0);
        
        try {
            Files.walk(targetDir).forEach(path -> {
                String pathStr = path.toAbsolutePath().toString();
                if (pathStr.contains("/.git") || pathStr.contains("/.gradle") || pathStr.contains("/.repo-dna")) {
                    return;
                }
                if (Files.isDirectory(path)) {
                    if (!path.equals(targetDir)) {
                        dirCount.incrementAndGet();
                    }
                } else {
                    fileCount.incrementAndGet();
                }
            });
        } catch (IOException e) {
            throw new CommandException("Failed to scan directory files: " + e.getMessage(), e);
        }

        boolean isJava = false;
        String javaVersion = "Not detected";

        try {
            isJava = Files.walk(targetDir)
                .filter(p -> !Files.isDirectory(p))
                .map(p -> p.toAbsolutePath().toString())
                .filter(p -> !p.contains("/.git") && !p.contains("/.gradle") && !p.contains("/.repo-dna"))
                .anyMatch(p -> p.endsWith(".java"));

            if (isGradle) {
                Path buildFile = targetDir.resolve("build.gradle.kts");
                if (!Files.exists(buildFile)) {
                    buildFile = targetDir.resolve("build.gradle");
                }
                if (Files.exists(buildFile)) {
                    String content = Files.readString(buildFile);
                    if (content.contains("sourceCompatibility =")) {
                        javaVersion = extractVersion(content, "sourceCompatibility =");
                    } else if (content.contains("JavaLanguageVersion.of(")) {
                        javaVersion = extractVersion(content, "JavaLanguageVersion.of(");
                    }
                }
            } else if (isMaven) {
                Path pom = targetDir.resolve("pom.xml");
                if (Files.exists(pom)) {
                    String content = Files.readString(pom);
                    if (content.contains("<maven.compiler.source>")) {
                        javaVersion = extractTagValue(content, "maven.compiler.source");
                    }
                }
            }
        } catch (IOException e) {
            // fallback gracefully
        }

        System.out.println("Repository path: " + targetDir);
        System.out.println("Project type:    " + (isJava ? "Java Project" : "General Project"));
        System.out.println("Build system:    " + buildSystem);
        System.out.println("Java version:    " + javaVersion);
        System.out.println("File count:      " + fileCount.get());
        System.out.println("Directory count: " + dirCount.get());
        return 0;
    }

    private String extractVersion(String content, String key) {
        int index = content.indexOf(key);
        if (index == -1) return "Not detected";
        int start = index + key.length();
        int end = content.indexOf("\n", start);
        if (end == -1) end = content.length();
        return content.substring(start, end).replaceAll("['\"() ;=]", "").trim();
    }

    private String extractTagValue(String content, String tag) {
        String startTag = "<" + tag + ">";
        String endTag = "</" + tag + ">";
        int start = content.indexOf(startTag);
        if (start == -1) return "Not detected";
        int end = content.indexOf(endTag, start);
        if (end == -1) return "Not detected";
        return content.substring(start + startTag.length(), end).trim();
    }
}
