package com.repodna.scanner.detector;

import com.repodna.scanner.model.GitInfo;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Executes git commands to analyze local repository branch and history metadata.
 */
public class GitAnalyzer {

    /**
     * Extracts git statistics. Falls back to default values if not a Git repository.
     */
    public static GitInfo analyze(Path repoPath) {
        boolean isGit = Files.exists(repoPath.resolve(".git"));
        if (!isGit) {
            return new GitInfo(false, "unknown", 0, 0, Collections.emptyList(), "unknown", "unknown");
        }

        String branch = runGitCommand(repoPath, "rev-parse", "--abbrev-ref", "HEAD");
        if (branch.isEmpty()) branch = "unknown";

        String commitCountStr = runGitCommand(repoPath, "rev-list", "--count", "HEAD");
        int commitCount = 0;
        try {
            commitCount = Integer.parseInt(commitCountStr.trim());
        } catch (NumberFormatException e) {}

        String tagCountStr = runGitCommand(repoPath, "tag");
        int tagCount = tagCountStr.isEmpty() ? 0 : tagCountStr.split("\n").length;

        String remotesStr = runGitCommand(repoPath, "remote", "-v");
        List<String> remoteUrls = new ArrayList<>();
        if (!remotesStr.isEmpty()) {
            for (String line : remotesStr.split("\n")) {
                if (line.contains("(fetch)")) {
                    String[] parts = line.split("\\s+");
                    if (parts.length > 1) {
                        remoteUrls.add(parts[1]);
                    }
                }
            }
        }

        String defaultBranch = runGitCommand(repoPath, "symbolic-ref", "refs/remotes/origin/HEAD");
        if (!defaultBranch.isEmpty()) {
            int slashIndex = defaultBranch.lastIndexOf('/');
            if (slashIndex != -1) {
                defaultBranch = defaultBranch.substring(slashIndex + 1);
            }
        } else {
            defaultBranch = "unknown";
        }

        String lastCommitTs = runGitCommand(repoPath, "log", "-1", "--format=%cd");
        if (lastCommitTs.isEmpty()) lastCommitTs = "unknown";

        return new GitInfo(true, branch, commitCount, tagCount, remoteUrls, defaultBranch, lastCommitTs);
    }

    private static String runGitCommand(Path dir, String... args) {
        try {
            List<String> cmd = new ArrayList<>();
            cmd.add("git");
            Collections.addAll(cmd, args);

            ProcessBuilder pb = new ProcessBuilder(cmd)
                .directory(dir.toFile())
                .redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            if (!finished || process.exitValue() != 0) {
                return "";
            }
            return output.toString().trim();
        } catch (Exception e) {
            return "";
        }
    }
}
