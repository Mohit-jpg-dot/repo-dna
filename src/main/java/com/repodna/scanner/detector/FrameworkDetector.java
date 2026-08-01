package com.repodna.scanner.detector;

import com.repodna.scanner.model.FrameworkInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Deterministically detects software development frameworks based on manifest signatures and source annotations.
 */
public class FrameworkDetector {

    /**
     * Inspects configuration and source files for framework evidence.
     */
    public static List<FrameworkInfo> detect(Path repoPath, List<Path> files) {
        List<FrameworkInfo> frameworks = new ArrayList<>();

        List<String> springBootEvidence = new ArrayList<>();
        List<String> reactEvidence = new ArrayList<>();
        List<String> nextEvidence = new ArrayList<>();
        List<String> expressEvidence = new ArrayList<>();
        List<String> nestEvidence = new ArrayList<>();
        List<String> djangoEvidence = new ArrayList<>();
        List<String> fastapiEvidence = new ArrayList<>();
        List<String> laravelEvidence = new ArrayList<>();
        List<String> quarkusEvidence = new ArrayList<>();
        List<String> micronautEvidence = new ArrayList<>();

        for (Path file : files) {
            String fileName = file.getFileName().toString();
            try {
                if ("build.gradle.kts".equals(fileName) || "build.gradle".equals(fileName) || "pom.xml".equals(fileName)) {
                    String content = Files.readString(file);
                    if (content.contains("spring-boot-starter")) {
                        springBootEvidence.add("Detected spring-boot-starter in build file: " + fileName);
                    }
                    if (content.contains("io.quarkus")) {
                        quarkusEvidence.add("Detected io.quarkus in build file: " + fileName);
                    }
                    if (content.contains("io.micronaut")) {
                        micronautEvidence.add("Detected io.micronaut in build file: " + fileName);
                    }
                } else if ("package.json".equals(fileName)) {
                    String content = Files.readString(file);
                    if (content.contains("\"react\"")) {
                        reactEvidence.add("Detected \"react\" dependency in package.json");
                    }
                    if (content.contains("\"next\"")) {
                        nextEvidence.add("Detected \"next\" dependency in package.json");
                    }
                    if (content.contains("\"express\"")) {
                        expressEvidence.add("Detected \"express\" dependency in package.json");
                    }
                    if (content.contains("\"@nestjs/core\"")) {
                        nestEvidence.add("Detected \"@nestjs/core\" dependency in package.json");
                    }
                } else if ("requirements.txt".equals(fileName) || "pyproject.toml".equals(fileName)) {
                    String content = Files.readString(file);
                    if (content.toLowerCase().contains("django")) {
                        djangoEvidence.add("Detected django dependency in " + fileName);
                    }
                    if (content.toLowerCase().contains("fastapi")) {
                        fastapiEvidence.add("Detected fastapi dependency in " + fileName);
                    }
                } else if ("composer.json".equals(fileName)) {
                    String content = Files.readString(file);
                    if (content.contains("laravel/framework")) {
                        laravelEvidence.add("Detected laravel/framework dependency in composer.json");
                    }
                } else if (fileName.endsWith(".java")) {
                    if (springBootEvidence.size() < 2) {
                        String content = Files.readString(file);
                        if (content.contains("@SpringBootApplication")) {
                            springBootEvidence.add("Detected @SpringBootApplication in " + fileName);
                        }
                    }
                }
            } catch (IOException e) {
                // Skip reading individual file on exception to proceed
            }
        }

        if (!springBootEvidence.isEmpty()) frameworks.add(new FrameworkInfo("Spring Boot", springBootEvidence));
        if (!quarkusEvidence.isEmpty()) frameworks.add(new FrameworkInfo("Quarkus", quarkusEvidence));
        if (!micronautEvidence.isEmpty()) frameworks.add(new FrameworkInfo("Micronaut", micronautEvidence));
        if (!reactEvidence.isEmpty()) frameworks.add(new FrameworkInfo("React", reactEvidence));
        if (!nextEvidence.isEmpty()) frameworks.add(new FrameworkInfo("Next.js", nextEvidence));
        if (!expressEvidence.isEmpty()) frameworks.add(new FrameworkInfo("Express", expressEvidence));
        if (!nestEvidence.isEmpty()) frameworks.add(new FrameworkInfo("NestJS", nestEvidence));
        if (!djangoEvidence.isEmpty()) frameworks.add(new FrameworkInfo("Django", djangoEvidence));
        if (!fastapiEvidence.isEmpty()) frameworks.add(new FrameworkInfo("FastAPI", fastapiEvidence));
        if (!laravelEvidence.isEmpty()) frameworks.add(new FrameworkInfo("Laravel", laravelEvidence));

        return frameworks;
    }
}
