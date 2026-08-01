package com.repodna.scanner;

import com.repodna.scanner.detector.*;
import com.repodna.scanner.model.*;
import com.repodna.util.DirectoryValidator;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

/**
 * Traverses repository files once, computes structure statistics, and runs detectors.
 */
public class RepositoryScanner {
    private static final Set<String> IGNORED_DIR_NAMES = Set.of(
        "build", "target", "node_modules", ".gradle", ".git", "out", "dist", "bin", ".repo-dna"
    );

    /**
     * Walks target directory, building and returning a complete ScannerResult.
     */
    public static ScannerResult scan(Path repoPath) {
        Path targetDir = repoPath.toAbsolutePath().normalize();
        DirectoryValidator.validate(targetDir);

        List<Path> files = new ArrayList<>();
        List<Path> directories = new ArrayList<>();
        
        Map<String, Integer> dirFileCounts = new HashMap<>();
        Map<String, Integer> extCounts = new HashMap<>();
        
        int[] maxDepth = {0};
        long[] totalDepth = {0};
        long[] totalFilenameLength = {0};

        try {
            Files.walkFileTree(targetDir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    String name = dir.getFileName() != null ? dir.getFileName().toString() : "";
                    if (IGNORED_DIR_NAMES.contains(name)) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    if (!dir.equals(targetDir)) {
                        directories.add(dir);
                        int depth = targetDir.relativize(dir).getNameCount();
                        if (depth > maxDepth[0]) {
                            maxDepth[0] = depth;
                        }
                        totalDepth[0] += depth;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    files.add(file);
                    
                    Path parent = file.getParent();
                    if (parent != null) {
                        String relParent = targetDir.relativize(parent).toString();
                        if (relParent.isEmpty()) relParent = ".";
                        dirFileCounts.put(relParent, dirFileCounts.getOrDefault(relParent, 0) + 1);
                    }

                    totalFilenameLength[0] += file.getFileName().toString().length();

                    String fileName = file.getFileName().toString();
                    int dotIndex = fileName.lastIndexOf('.');
                    if (dotIndex != -1 && dotIndex < fileName.length() - 1) {
                        String ext = fileName.substring(dotIndex + 1).toLowerCase();
                        extCounts.put(ext, extCounts.getOrDefault(ext, 0) + 1);
                    } else {
                        extCounts.put("no-extension", extCounts.getOrDefault("no-extension", 0) + 1);
                    }

                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            // Fallback gracefully on IO exceptions
        }

        List<Map.Entry<String, Integer>> sortedDirs = new ArrayList<>(dirFileCounts.entrySet());
        sortedDirs.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
        List<String> largestDirs = new ArrayList<>();
        for (int i = 0; i < Math.min(5, sortedDirs.size()); i++) {
            Map.Entry<String, Integer> entry = sortedDirs.get(i);
            largestDirs.add(entry.getKey() + " (" + entry.getValue() + " files)");
        }

        double avgDepth = directories.isEmpty() ? 0.0 : (double) totalDepth[0] / directories.size();
        avgDepth = Math.round(avgDepth * 100.0) / 100.0;
        
        double avgFilenameLength = files.isEmpty() ? 0.0 : (double) totalFilenameLength[0] / files.size();
        avgFilenameLength = Math.round(avgFilenameLength * 100.0) / 100.0;

        RepositoryStatistics stats = new RepositoryStatistics(
            files.size(),
            directories.size(),
            maxDepth[0],
            avgDepth,
            largestDirs,
            extCounts,
            avgFilenameLength
        );

        // Delegate paths list to detectors
        GitInfo gitInfo = GitAnalyzer.analyze(targetDir);
        List<LanguageInfo> languages = LanguageDetector.detect(files);
        LanguageInfo primaryLanguage = languages.isEmpty() ? new LanguageInfo("None", 0, 0) : languages.get(0);
        List<FrameworkInfo> frameworks = FrameworkDetector.detect(targetDir, files);
        BuildSystemInfo buildSystem = BuildSystemDetector.detect(files);
        DocumentationInfo docs = DocDetector.detect(files);
        List<String> configs = ConfigDetector.detect(files);

        String repoName = targetDir.getFileName() != null ? targetDir.getFileName().toString() : "unknown";

        return new ScannerResult(
            repoName,
            targetDir,
            gitInfo,
            languages,
            primaryLanguage,
            frameworks,
            buildSystem,
            stats,
            docs,
            configs
        );
    }
}
