package com.repodna.cli;

import com.repodna.RepoDnaCli;
import com.repodna.util.Console;
import picocli.CommandLine.Command;
import picocli.CommandLine.ParentCommand;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Checks environment and diagnoses potential issues.
 */
@Command(
    name = "doctor",
    description = "Check environment and diagnose issues",
    mixinStandardHelpOptions = true
)
public class DoctorCommand implements Callable<Integer> {

    @ParentCommand
    private RepoDnaCli parent;

    @Override
    public Integer call() {
        Console.header("RepoDNA System Doctor");
        Console.info("Running system and environment checks...");
        boolean allPassed = true;

        // 1. Check Java version
        boolean javaPassed = checkJavaVersion();
        allPassed &= javaPassed;

        // 2. Check git availability
        boolean gitPassed = checkGit();
        allPassed &= gitPassed;

        // 3. Check if current directory is a git repo
        Path repoDir = parent.getRepoDir();
        boolean isGitRepo = Files.exists(repoDir.resolve(".git"));
        if (isGitRepo) {
            Console.success("Current directory is a Git repository.");
        } else {
            Console.error("Current directory is NOT a Git repository.");
            Console.info("Hint: Initialize a Git repository with 'git init'.");
        }
        allPassed &= isGitRepo;

        // 4. Check if .repodna/ directory exists
        boolean hasRepoDna = Files.exists(repoDir.resolve(".repodna"));
        if (hasRepoDna) {
            Console.success(".repodna/ configuration directory exists.");
        } else {
            Console.warning(".repodna/ configuration directory is missing.");
            Console.info("Hint: Run 'repo-dna init' to initialize RepoDNA.");
        }
        allPassed &= hasRepoDna;

        // 5. Check build tool
        boolean hasBuildTool = Files.exists(repoDir.resolve("build.gradle")) 
                            || Files.exists(repoDir.resolve("build.gradle.kts")) 
                            || Files.exists(repoDir.resolve("pom.xml"));
        if (hasBuildTool) {
            Console.success("Supported build tool detected (Gradle/Maven).");
        } else {
            Console.error("No supported build tool detected (build.gradle/pom.xml).");
            Console.info("Hint: Make sure you run RepoDNA at the root of a Maven or Gradle project.");
        }
        allPassed &= hasBuildTool;

        Console.blank();
        if (allPassed) {
            Console.success("All environment checks passed! RepoDNA is ready to use.");
            return 0;
        } else {
            Console.warning("Some environment checks failed. Please review the issues above.");
            return 1;
        }
    }

    private boolean checkJavaVersion() {
        String version = System.getProperty("java.version");
        boolean passed = false;
        
        try {
            Matcher matcher = Pattern.compile("^(\\d+)").matcher(version);
            if (matcher.find()) {
                int major = Integer.parseInt(matcher.group(1));
                passed = major >= 21;
            }
        } catch (Exception e) {
            // fallback
        }
        
        if (passed) {
            Console.success("Java version >= 21 (Found: " + version + ")");
        } else {
            Console.error("Java version < 21 (Found: " + version + "). Java 21+ is required.");
        }
        return passed;
    }

    private boolean checkGit() {
        boolean passed = false;
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"git", "--version"});
            process.waitFor();
            passed = process.exitValue() == 0;
        } catch (Exception e) {
            // failed to execute
        }
        
        if (passed) {
            Console.success("Git CLI is available.");
        } else {
            Console.error("Git CLI is not available in system PATH.");
        }
        return passed;
    }
}
