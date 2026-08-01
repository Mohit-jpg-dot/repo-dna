package com.repodna.scanner;

import com.repodna.model.SourceFile;
import java.nio.file.Path;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;

public class FileClassifier {
    /**
     * Classify a file based on its path and extension.
     */
    public static SourceFile classify(Path rootPath, Path filePath) {
        Path relativePath = rootPath.relativize(filePath);
        String pathStr = relativePath.toString().replace("\\", "/");
        String fileName = filePath.getFileName().toString();
        
        SourceFile.FileType type = SourceFile.FileType.OTHER;
        String lang = detectLanguage(filePath);
        
        boolean isJava = fileName.endsWith(".java");
        
        if (isJava) {
            if (pathStr.contains("src/test/") || pathStr.contains("/test/") || pathStr.contains("/tests/") || pathStr.contains("/__tests__/")) {
                type = SourceFile.FileType.TEST;
            } else if (pathStr.contains("src/main/")) {
                type = SourceFile.FileType.SOURCE;
            } else {
                type = SourceFile.FileType.SOURCE;
            }
        } else if (isDependencyManifest(fileName)) {
            type = SourceFile.FileType.DEPENDENCY_MANIFEST;
        } else if (isBuild(fileName)) {
            type = SourceFile.FileType.BUILD;
        } else if (isConfig(fileName, pathStr)) {
            type = SourceFile.FileType.CONFIG;
        } else if (isDocumentation(fileName)) {
            type = SourceFile.FileType.DOCUMENTATION;
        } else if (pathStr.contains("resources/")) {
            type = SourceFile.FileType.RESOURCE;
        }

        try {
            BasicFileAttributes attrs = Files.readAttributes(filePath, BasicFileAttributes.class);
            return new SourceFile(
                relativePath,
                filePath,
                type,
                lang,
                attrs.size(),
                attrs.lastModifiedTime().toInstant()
            );
        } catch (IOException e) {
            System.err.println("Error reading attributes for " + filePath + ": " + e.getMessage());
            return new SourceFile(relativePath, filePath, type, lang, 0, Instant.now());
        }
    }
    
    private static boolean isDependencyManifest(String fileName) {
        return fileName.equals("pom.xml") || fileName.equals("build.gradle") || 
               fileName.equals("package.json") || fileName.equals("requirements.txt");
    }
    
    private static boolean isBuild(String fileName) {
        return fileName.equals("build.gradle") || fileName.equals("build.gradle.kts") || 
               fileName.equals("pom.xml") || fileName.equals("settings.gradle") || 
               fileName.equals("settings.gradle.kts") || fileName.equals("Makefile") || 
               fileName.equals("Dockerfile");
    }
    
    private static boolean isConfig(String fileName, String pathStr) {
        boolean isExtConfig = fileName.endsWith(".properties") || fileName.endsWith(".yml") || 
                              fileName.endsWith(".yaml") || (fileName.endsWith(".xml") && !fileName.equals("pom.xml"));
        boolean isJsonConfig = fileName.endsWith(".json") && (pathStr.contains("src/main/resources/") || pathStr.contains("config/"));
        return isExtConfig || isJsonConfig;
    }
    
    private static boolean isDocumentation(String fileName) {
        return fileName.endsWith(".md") || fileName.endsWith(".txt") || 
               fileName.endsWith(".adoc") || fileName.endsWith(".rst");
    }
    
    /**
     * Detect the programming language from a file extension.
     */
    public static String detectLanguage(Path filePath) {
        String fileName = filePath.getFileName().toString();
        int dotIdx = fileName.lastIndexOf('.');
        if (dotIdx == -1) return "unknown";
        String ext = fileName.substring(dotIdx + 1).toLowerCase();
        
        return switch (ext) {
            case "java" -> "java";
            case "xml" -> "xml";
            case "properties" -> "properties";
            case "yml", "yaml" -> "yaml";
            case "json" -> "json";
            case "md" -> "markdown";
            case "sql" -> "sql";
            case "kt", "kts" -> "kotlin";
            case "groovy" -> "groovy";
            case "js" -> "javascript";
            case "ts" -> "typescript";
            case "py" -> "python";
            case "go" -> "go";
            case "rs" -> "rust";
            case "c", "h" -> "c";
            case "cpp", "hpp" -> "cpp";
            default -> "unknown";
        };
    }
}
