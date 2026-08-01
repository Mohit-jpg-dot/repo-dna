package com.repodna.scanner;

import com.repodna.model.ProjectInfo;
import java.nio.file.Path;
import java.nio.file.Files;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProjectDetector {

    /**
     * Detect project information from the repository root.
     */
    public static ProjectInfo detect(Path rootPath) {
        String name = rootPath.getFileName().toString();
        String buildTool = "none";
        String framework = "none";
        boolean multiModule = false;
        String javaVersion = "unknown";
        
        Path pomXml = rootPath.resolve("pom.xml");
        Path buildGradle = rootPath.resolve("build.gradle");
        Path buildGradleKts = rootPath.resolve("build.gradle.kts");
        Path settingsGradle = rootPath.resolve("settings.gradle");
        Path settingsGradleKts = rootPath.resolve("settings.gradle.kts");
        
        if (Files.exists(pomXml)) {
            buildTool = "maven";
            try {
                String pomContent = Files.readString(pomXml);
                
                Matcher artifactIdMatcher = Pattern.compile("<artifactId>(.*?)</artifactId>").matcher(pomContent);
                if (artifactIdMatcher.find()) {
                    name = artifactIdMatcher.group(1);
                }
                
                if (pomContent.contains("<modules>")) {
                    multiModule = true;
                }
                
                Matcher javaVersionMatcher = Pattern.compile("<maven.compiler.source>(.*?)</maven.compiler.source>").matcher(pomContent);
                if (javaVersionMatcher.find()) {
                    javaVersion = javaVersionMatcher.group(1);
                } else if (pomContent.contains("<java.version>")) {
                    Matcher jvMatcher = Pattern.compile("<java.version>(.*?)</java.version>").matcher(pomContent);
                    if (jvMatcher.find()) {
                        javaVersion = jvMatcher.group(1);
                    }
                }
                
                if (pomContent.contains("spring-boot-starter")) {
                    framework = "spring-boot";
                } else if (pomContent.contains("org.springframework")) {
                    framework = "spring";
                }
                
            } catch (IOException e) {
                System.err.println("Error reading pom.xml: " + e.getMessage());
            }
        } else if (Files.exists(buildGradle) || Files.exists(buildGradleKts)) {
            buildTool = "gradle";
            Path gradleFile = Files.exists(buildGradleKts) ? buildGradleKts : buildGradle;
            try {
                String buildContent = Files.readString(gradleFile);
                
                if (buildContent.contains("spring-boot-starter") || buildContent.contains("org.springframework.boot")) {
                    framework = "spring-boot";
                } else if (buildContent.contains("org.springframework")) {
                    framework = "spring";
                }
                
                Matcher javaVersionMatcher = Pattern.compile("JavaLanguageVersion\\.of\\((.*?)\\)").matcher(buildContent);
                if (javaVersionMatcher.find()) {
                    javaVersion = javaVersionMatcher.group(1).replace("\"", "").replace("'", "");
                } else {
                    Matcher scMatcher = Pattern.compile("sourceCompatibility\\s*=?\\s*['\"]?(.*?)['\"]?").matcher(buildContent);
                    if (scMatcher.find()) {
                        javaVersion = scMatcher.group(1);
                    }
                }
                
            } catch (IOException e) {
                System.err.println("Error reading gradle build file: " + e.getMessage());
            }
        }
        
        if (buildTool.equals("gradle") && (Files.exists(settingsGradle) || Files.exists(settingsGradleKts))) {
            Path settingsFile = Files.exists(settingsGradleKts) ? settingsGradleKts : settingsGradle;
            try {
                String settingsContent = Files.readString(settingsFile);
                Matcher nameMatcher = Pattern.compile("rootProject\\.name\\s*=\\s*['\"](.*?)['\"]").matcher(settingsContent);
                if (nameMatcher.find()) {
                    name = nameMatcher.group(1);
                }
                
                if (settingsContent.contains("include(") || settingsContent.contains("include '") || settingsContent.contains("include \"")) {
                    multiModule = true;
                }
            } catch (IOException e) {
                System.err.println("Error reading gradle settings file: " + e.getMessage());
            }
        }

        if (framework.equals("none")) {
            Path mainJavaDir = rootPath.resolve("src/main/java");
            if (Files.exists(mainJavaDir)) {
                try {
                    boolean hasSpringBoot = Files.walk(mainJavaDir, 5)
                            .filter(p -> p.toString().endsWith(".java"))
                            .limit(20)
                            .anyMatch(p -> {
                                try {
                                    return Files.readString(p).contains("@SpringBootApplication");
                                } catch (IOException e) {
                                    return false;
                                }
                            });
                    if (hasSpringBoot) {
                        framework = "spring-boot";
                    }
                } catch (IOException e) {
                    System.err.println("Error scanning for spring boot classes: " + e.getMessage());
                }
            }
        }

        return new ProjectInfo(
            name,
            rootPath,
            "java",
            framework,
            buildTool,
            multiModule,
            new ArrayList<>(),
            javaVersion,
            Instant.now()
        );
    }
}
