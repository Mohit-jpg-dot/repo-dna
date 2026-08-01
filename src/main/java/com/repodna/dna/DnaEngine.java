package com.repodna.dna;

import com.repodna.discovery.PatternDiscoveryEngine;
import com.repodna.discovery.model.*;
import com.repodna.graph.GraphBuilder;
import com.repodna.graph.RepositoryKnowledgeGraph;
import com.repodna.parser.ParserEngine;
import com.repodna.parser.model.ProjectModel;
import com.repodna.scanner.RepositoryScanner;
import com.repodna.scanner.model.ScannerResult;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Orchestrator that coordinates the full repository analysis pipeline:
 * Scanner -> Parser -> Knowledge Graph -> Pattern Discovery -> DNA Profile.
 */
public class DnaEngine {

    /**
     * Executes the entire engineering intelligence pipeline on the target repository.
     */
    public static DnaProfile analyze(Path repoPath) {
        Path targetDir = repoPath.toAbsolutePath().normalize();

        // 1. Scan Repository Metadata
        ScannerResult scannerResult = RepositoryScanner.scan(targetDir);

        // 2. Discover Java Files and Source Roots
        List<Path> javaFiles = findJavaFiles(targetDir);
        List<Path> srcRoots = resolveSourceRoots(targetDir, javaFiles);

        // 3. Parse AST if Java files are present
        ProjectModel projectModel;
        if (!javaFiles.isEmpty()) {
            ParserEngine parserEngine = new ParserEngine(srcRoots);
            projectModel = parserEngine.parse(scannerResult.repoName(), javaFiles);
        } else {
            projectModel = new ProjectModel(scannerResult.repoName(), Collections.emptyList(), Collections.emptyList());
        }

        // 4. Build Repository Knowledge Graph (RKG)
        RepositoryKnowledgeGraph graph = GraphBuilder.build(scannerResult, projectModel);

        // 5. Discover Engineering Patterns
        PatternDiscoveryEngine patternEngine = new PatternDiscoveryEngine(graph);
        List<Pattern> patterns = patternEngine.discover();

        // 6. Cluster Patterns and Build Summary
        PatternSummary summary = buildSummary(patterns);

        // 7. Assemble DNA Profile
        return new DnaProfile(
            scannerResult,
            projectModel,
            graph,
            patterns,
            summary,
            Instant.now()
        );
    }

    private static List<Path> findJavaFiles(Path repoPath) {
        List<Path> javaFiles = new ArrayList<>();
        try {
            Files.walkFileTree(repoPath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    String name = dir.getFileName() != null ? dir.getFileName().toString() : "";
                    if (Set.of("build", "target", "node_modules", ".gradle", ".git", "out", "dist", "bin", ".repo-dna", ".repodna").contains(name)) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (file.toString().endsWith(".java")) {
                        javaFiles.add(file);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            // fallback
        }
        return javaFiles;
    }

    private static List<Path> resolveSourceRoots(Path repoPath, List<Path> javaFiles) {
        List<Path> srcRoots = new ArrayList<>();
        Path srcMainJava = repoPath.resolve("src/main/java");
        if (Files.exists(srcMainJava)) {
            srcRoots.add(srcMainJava);
        }
        Path srcTestJava = repoPath.resolve("src/test/java");
        if (Files.exists(srcTestJava)) {
            srcRoots.add(srcTestJava);
        }

        // If no standard structure, find parent directories of Java files
        if (srcRoots.isEmpty()) {
            Set<Path> parentDirs = new HashSet<>();
            for (Path file : javaFiles) {
                Path parent = file.getParent();
                if (parent != null) {
                    parentDirs.add(parent);
                }
            }
            srcRoots.addAll(parentDirs);
        }

        if (srcRoots.isEmpty()) {
            srcRoots.add(repoPath);
        }
        return srcRoots;
    }

    private static PatternSummary buildSummary(List<Pattern> patterns) {
        int total = patterns.size();
        double sumScore = 0.0;
        Map<PatternCategory, Integer> countByCategory = new HashMap<>();

        for (Pattern p : patterns) {
            sumScore += p.confidence().score();
            countByCategory.put(p.category(), countByCategory.getOrDefault(p.category(), 0) + 1);
        }

        double overallConfidence = total == 0 ? 0.0 : sumScore / total;
        overallConfidence = Math.round(overallConfidence * 100.0) / 100.0;

        // Group related patterns into clusters
        List<PatternGroup> groups = new ArrayList<>();
        Map<PatternCategory, List<Pattern>> grouped = patterns.stream()
            .collect(Collectors.groupingBy(Pattern::category));

        for (Map.Entry<PatternCategory, List<Pattern>> entry : grouped.entrySet()) {
            groups.add(new PatternGroup(
                "group-" + entry.getKey().name().toLowerCase().replace('_', '-'),
                entry.getKey().name() + " Group",
                "Clustered conventions for category " + entry.getKey(),
                entry.getValue()
            ));
        }

        return new PatternSummary(
            total,
            overallConfidence,
            countByCategory,
            groups
        );
    }
}
