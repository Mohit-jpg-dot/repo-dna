package com.repodna.scanner.detector;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Detects presence of standard configuration files and hidden IDE/CI structures.
 */
public class ConfigDetector {
    private static final Set<String> CONFIG_NAMES = Set.of(
        "application.properties", "application.yml", "application.yaml",
        ".env", "docker-compose.yml", "docker-compose.yaml", "Dockerfile",
        ".editorconfig", ".gitignore"
    );

    /**
     * Identifies configuration files and workflows from a list of paths.
     */
    public static List<String> detect(List<Path> files) {
        List<String> presentConfigs = new ArrayList<>();
        for (Path file : files) {
            String fileName = file.getFileName().toString();
            String pathStr = file.toAbsolutePath().toString();

            if (CONFIG_NAMES.contains(fileName)) {
                presentConfigs.add(fileName);
            } else if (pathStr.contains("/.github/")) {
                if (!presentConfigs.contains(".github/")) {
                    presentConfigs.add(".github/");
                }
            } else if (pathStr.contains("/.idea/")) {
                if (!presentConfigs.contains(".idea/")) {
                    presentConfigs.add(".idea/");
                }
            } else if (pathStr.contains("/.vscode/")) {
                if (!presentConfigs.contains(".vscode/")) {
                    presentConfigs.add(".vscode/");
                }
            }
        }
        return presentConfigs;
    }
}
