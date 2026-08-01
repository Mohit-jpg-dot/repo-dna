package com.repodna.commands;

import com.repodna.dna.DnaEngine;
import com.repodna.dna.DnaProfile;
import com.repodna.dna.DnaReportGenerator;
import com.repodna.discovery.model.Pattern;
import com.repodna.discovery.model.PatternViolation;
import com.repodna.scanner.model.LanguageInfo;
import com.repodna.util.DirectoryValidator;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.List;

/**
 * Command to execute the Repository DNA Engine pipeline and print results.
 */
@Command(
    name = "analyze",
    description = "Validate target directory, parse code, build knowledge graph, and discover engineering DNA patterns",
    mixinStandardHelpOptions = true
)
public class AnalyzeCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "The target directory to analyze", defaultValue = ".")
    private Path dir = Path.of(".");

    @Override
    public Integer call() {
        Path targetDir = dir.toAbsolutePath().normalize();
        DirectoryValidator.validate(targetDir);

        System.out.println("Analyzing repository...");
        DnaProfile profile = DnaEngine.analyze(targetDir);

        // Generate Reports
        try {
            DnaReportGenerator.generateReports(profile, targetDir);
        } catch (IOException e) {
            System.err.println("Error generating markdown reports: " + e.getMessage());
        }

        // Detect Java Version for Display
        String javaVersion = "Not detected";
        if (profile.scannerResult().primaryLanguage().name().equalsIgnoreCase("Java")) {
            javaVersion = detectJavaVersion(targetDir, profile.scannerResult().buildSystem().name());
        }

        // Print Summary to Terminal
        System.out.println("\nRepository Summary");
        System.out.println("Repository Name:        " + profile.scannerResult().repoName());
        System.out.println("Git Repository:         " + (profile.scannerResult().gitInfo().isGitRepo() ? "Yes" : "No"));
        System.out.println("Primary Language:       " + profile.scannerResult().primaryLanguage().name());
        System.out.println("Framework:              " + (profile.scannerResult().frameworks().isEmpty() ? "None" : profile.scannerResult().frameworks().get(0).name()));
        System.out.println("Build System:           " + profile.scannerResult().buildSystem().name());
        System.out.println("Files:                  " + profile.scannerResult().stats().fileCount());
        System.out.println("Directories:            " + profile.scannerResult().stats().directoryCount());
        System.out.println("Java Version:           " + javaVersion);

        System.out.println("\nEngineering DNA Summary");
        System.out.println("Overall Confidence:     " + (profile.summary().overallConfidence() * 100) + "%");
        System.out.println("Total Discovered Patterns: " + profile.summary().totalPatterns());

        System.out.println("\nDominant Patterns (>= 70% Confidence):");
        List<Pattern> dominant = profile.patterns().stream()
            .filter(p -> p.confidence().score() >= 0.7)
            .toList();
        if (dominant.isEmpty()) {
            System.out.println("  None");
        } else {
            for (Pattern p : dominant) {
                System.out.printf("  - %s: %s (Confidence: %.0f%%)\n", p.name(), p.description(), p.confidence().score() * 100);
            }
        }

        System.out.println("\nAnomalies / Violations:");
        List<PatternViolation> anomalies = profile.patterns().stream()
            .flatMap(p -> p.violationsList().stream())
            .toList();
        if (anomalies.isEmpty()) {
            System.out.println("  None");
        } else {
            for (PatternViolation v : anomalies) {
                System.out.printf("  - %s: %s\n", v.nodeId(), v.description());
            }
        }

        System.out.println("\nGenerated Markdown Reports:");
        System.out.println("  ✓ REPO_DNA.md");
        System.out.println("  ✓ AGENTS.md");
        System.out.println("  ✓ ARCHITECTURE.md");
        System.out.println("  ✓ PROJECT_RULES.md");

        return 0;
    }

    private String detectJavaVersion(Path targetDir, String buildSystemName) {
        try {
            if (buildSystemName.contains("Gradle")) {
                Path buildFile = targetDir.resolve("build.gradle.kts");
                if (!Files.exists(buildFile)) {
                    buildFile = targetDir.resolve("build.gradle");
                }
                if (Files.exists(buildFile)) {
                    String content = Files.readString(buildFile);
                    if (content.contains("sourceCompatibility =")) {
                        return extractVersion(content, "sourceCompatibility =");
                    } else if (content.contains("JavaLanguageVersion.of(")) {
                        return extractVersion(content, "JavaLanguageVersion.of(");
                    }
                }
            } else if ("Maven".equals(buildSystemName)) {
                Path pom = targetDir.resolve("pom.xml");
                if (Files.exists(pom)) {
                    String content = Files.readString(pom);
                    if (content.contains("<maven.compiler.source>")) {
                        return extractTagValue(content, "maven.compiler.source");
                    }
                }
            }
        } catch (IOException e) {
            // fallback
        }
        return "Not detected";
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
