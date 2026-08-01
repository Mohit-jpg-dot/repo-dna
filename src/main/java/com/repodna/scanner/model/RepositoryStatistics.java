package com.repodna.scanner.model;

import java.util.List;
import java.util.Map;

/**
 * Immutable representation of directory and file level filesystem statistics.
 */
public record RepositoryStatistics(
    int fileCount,
    int directoryCount,
    int maxDepth,
    double avgDepth,
    List<String> largestDirectories,
    Map<String, Integer> fileTypeDistribution,
    double avgFilenameLength
) {}
