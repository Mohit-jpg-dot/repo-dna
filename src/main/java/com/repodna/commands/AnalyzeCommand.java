package com.repodna.commands;

import com.repodna.scanner.RepositoryScanner;
import com.repodna.scanner.model.*;
import com.repodna.util.DirectoryValidator;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

/**
 * Command to execute Repository Scanner and print repository layout metadata.
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

        ScannerResult result = RepositoryScanner.scan(targetDir);

        boolean hasReadme = result.documentation().presentDocs().stream()
            .anyMatch(d -> d.toUpperCase().contains("README"));
            
        boolean hasLicense = result.documentation().presentDocs().stream()
            .anyMatch(d -> d.toUpperCase().contains("LICENSE"));

        boolean hasGitignore = result.configs().contains(".gitignore");
        boolean hasDocker = result.configs().contains("Dockerfile") || result.configs().contains("docker-compose.yml") || result.configs().contains("docker-compose.yaml");
        boolean hasGithubActions = result.configs().contains(".github/");

        String primaryLanguage = result.primaryLanguage().name();
        String languagesStr = result.languages().isEmpty() ? "None" : 
            String.join(", ", result.languages().stream().map(LanguageInfo::name).toList());

        String framework = result.frameworks().isEmpty() ? "None" : result.frameworks().get(0).name();
        
        String javaVersion = "Not detected";
        if ("Java".equals(primaryLanguage)) {
            javaVersion = detectJavaVersion(targetDir, result.buildSystem());
        }

        String projectSize = "Small";
        int fileCount = result.stats().fileCount();
        if (fileCount > 1000) {
            projectSize = "Large";
        } else if (fileCount > 100) {
            projectSize = "Medium";
        }

        System.out.println("Repository Summary");
        System.out.println("Repository Name:        " + result.repoName());
        System.out.println("Git Repository:         " + (result.gitInfo().isGitRepo() ? "Yes" : "No"));
        System.out.println("Languages:              " + languagesStr);
        System.out.println("Primary Language:       " + primaryLanguage);
        System.out.println("Framework:              " + framework);
        System.out.println("Build System:           " + result.buildSystem().name());
        System.out.println("Files:                  " + fileCount);
        System.out.println("Directories:            " + result.stats().directoryCount());
        System.out.println("README:                 " + (hasReadme ? "Present" : "Missing"));
        System.out.println("License:                " + (hasLicense ? "Present" : "None"));
        System.out.println("Git Ignore:             " + (hasGitignore ? "Present" : "Missing"));
        System.out.println("Docker:                 " + (hasDocker ? "Detected" : "None"));
        System.out.println("CI:                     " + (hasGithubActions ? "GitHub Actions" : "None"));
        System.out.println("Package Manager:        " + result.buildSystem().packageManager());
        System.out.println("Java Version:           " + javaVersion);
        System.out.println("Estimated Project Size: " + projectSize);
        
        return 0;
    }

    private String detectJavaVersion(Path targetDir, BuildSystemInfo buildSystem) {
        try {
            if (buildSystem.name().contains("Gradle")) {
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
            } else if ("Maven".equals(buildSystem.name())) {
                Path pom = targetDir.resolve("pom.xml");
                if (Files.exists(pom)) {
                    String content = Files.readString(pom);
                    if (content.contains("<maven.compiler.source>")) {
                        return extractTagValue(content, "maven.compiler.source");
                    }
                }
            }
        } catch (IOException e) {
            // fallback gracefully
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
