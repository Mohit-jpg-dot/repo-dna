package com.repodna.scanner.model;

import java.util.List;

/**
 * Immutable representation of Git repository metadata.
 */
public record GitInfo(
    boolean isGitRepo,
    String currentBranch,
    int commitCount,
    int tagCount,
    List<String> remoteUrls,
    String defaultBranch,
    String lastCommitTimestamp
) {}
