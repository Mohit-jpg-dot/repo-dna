package com.repodna.scanner.detector;

import com.repodna.scanner.model.BuildSystemInfo;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Detects build system and package manager based on workspace configuration files.
 */
public class BuildSystemDetector {

    /**
     * Resolves the primary build tool and list of config files.
     */
    public static BuildSystemInfo detect(List<Path> files) {
        String name = "None";
        String packageManager = "None";
        List<String> buildFiles = new ArrayList<>();

        boolean hasGradleKts = false;
        boolean hasGradle = false;
        boolean hasMaven = false;
        boolean hasNpm = false;
        boolean hasCargo = false;
        boolean hasGo = false;
        boolean hasComposer = false;
        boolean hasPip = false;
        boolean hasPoetry = false;

        boolean hasYarnLock = false;
        boolean hasPnpmLock = false;
        boolean hasPackageLock = false;

        for (Path file : files) {
            String fileName = file.getFileName().toString();
            switch (fileName) {
                case "build.gradle.kts" -> { hasGradleKts = true; buildFiles.add(fileName); }
                case "build.gradle" -> { hasGradle = true; buildFiles.add(fileName); }
                case "pom.xml" -> { hasMaven = true; buildFiles.add(fileName); }
                case "package.json" -> { hasNpm = true; buildFiles.add(fileName); }
                case "Cargo.toml" -> { hasCargo = true; buildFiles.add(fileName); }
                case "go.mod" -> { hasGo = true; buildFiles.add(fileName); }
                case "composer.json" -> { hasComposer = true; buildFiles.add(fileName); }
                case "requirements.txt" -> { hasPip = true; buildFiles.add(fileName); }
                case "pyproject.toml" -> { hasPoetry = true; buildFiles.add(fileName); }
                case "yarn.lock" -> hasYarnLock = true;
                case "pnpm-lock.yaml" -> hasPnpmLock = true;
                case "package-lock.json" -> hasPackageLock = true;
            }
        }

        if (hasGradleKts) {
            name = "Gradle Kotlin DSL";
            packageManager = "Gradle";
        } else if (hasGradle) {
            name = "Gradle Groovy DSL";
            packageManager = "Gradle";
        } else if (hasMaven) {
            name = "Maven";
            packageManager = "Maven";
        } else if (hasNpm) {
            name = "npm";
            packageManager = "npm";
            if (hasYarnLock) packageManager = "Yarn";
            else if (hasPnpmLock) packageManager = "pnpm";
            else if (hasPackageLock) packageManager = "npm";
        } else if (hasCargo) {
            name = "Cargo";
            packageManager = "Cargo";
        } else if (hasGo) {
            name = "Go Modules";
            packageManager = "Go";
        } else if (hasComposer) {
            name = "Composer";
            packageManager = "Composer";
        } else if (hasPoetry) {
            name = "Poetry";
            packageManager = "Poetry";
        } else if (hasPip) {
            name = "Pip";
            packageManager = "Pip";
        }

        return new BuildSystemInfo(name, packageManager, buildFiles);
    }
}
