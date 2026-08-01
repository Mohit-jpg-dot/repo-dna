package com.repodna.scanner;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class GitAnalyzer {
    private final Path rootPath;

    public GitAnalyzer(Path rootPath) {
        this.rootPath = rootPath;
    }

    /** Check if git is available and this is a git repo */
    public boolean isGitRepo() {
        if (!rootPath.resolve(".git").toFile().exists()) {
            return false;
        }
        List<String> output = runGitCommand("status", "--short");
        return output != null;
    }

    /** Get file change frequency (how many commits touched each file) */
    public Map<String, Integer> getFileChangeFrequency(int maxCommits) {
        Map<String, Integer> frequency = new HashMap<>();
        List<String> output = runGitCommand("log", "--format=", "--name-only", "-n", String.valueOf(maxCommits));
        if (output != null) {
            for (String file : output) {
                if (!file.trim().isEmpty()) {
                    frequency.put(file, frequency.getOrDefault(file, 0) + 1);
                }
            }
        }
        return frequency;
    }

    /** Get total commit count */
    public int getCommitCount() {
        List<String> output = runGitCommand("rev-list", "--count", "HEAD");
        if (output != null && !output.isEmpty()) {
            try {
                return Integer.parseInt(output.get(0).trim());
            } catch (NumberFormatException ignored) {}
        }
        return 0;
    }

    /** Get contributor count */
    public int getContributorCount() {
        List<String> output = runGitCommand("shortlog", "-sn", "HEAD");
        if (output != null) {
            return output.size();
        }
        return 0;
    }

    /** Get list of recently modified files (last N commits) */
    public List<String> getRecentlyModifiedFiles(int lastNCommits) {
        List<String> files = new ArrayList<>();
        List<String> output = runGitCommand("log", "--format=", "--name-only", "-n", String.valueOf(lastNCommits));
        if (output != null) {
            for (String file : output) {
                if (!file.trim().isEmpty() && !files.contains(file)) {
                    files.add(file);
                }
            }
        }
        return files;
    }

    /** Get branch name */
    public String getCurrentBranch() {
        List<String> output = runGitCommand("branch", "--show-current");
        if (output != null && !output.isEmpty()) {
            return output.get(0).trim();
        }
        return "";
    }

    private List<String> runGitCommand(String... args) {
        List<String> command = new ArrayList<>();
        command.add("git");
        command.addAll(List.of(args));

        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(rootPath.toFile());
            Process process = pb.start();

            boolean finished = process.waitFor(10, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return null;
            }

            if (process.exitValue() != 0) {
                return null;
            }

            List<String> lines = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    lines.add(line);
                }
            }
            return lines;
        } catch (Exception e) {
            System.err.println("Error running git command: " + e.getMessage());
            return null;
        }
    }
}
