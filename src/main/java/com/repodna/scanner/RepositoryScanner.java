package com.repodna.scanner;

import com.repodna.model.SourceFile;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.regex.Pattern;

public class RepositoryScanner {
    private final Path rootPath;
    private final boolean verbose;
    
    private static final Set<String> EXCLUDED_DIRS = Set.of(
            ".git", ".gradle", ".idea", ".vscode", "build", "target",
            "node_modules", "bin", "out", ".repodna", "__pycache__", ".settings", ".project"
    );

    public RepositoryScanner(Path rootPath, boolean verbose) {
        this.rootPath = rootPath;
        this.verbose = verbose;
    }

    /**
     * Scan the repository and return all discovered source files.
     * Respects .gitignore patterns and excludes common directories.
     */
    public List<SourceFile> scan() {
        List<SourceFile> files = new ArrayList<>();
        List<GitIgnoreRule> ignoreRules = loadGitIgnore();

        try {
            Files.walkFileTree(rootPath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    String dirName = dir.getFileName().toString();
                    if (EXCLUDED_DIRS.contains(dirName)) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    if (isIgnored(dir, true, ignoreRules)) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (isIgnored(file, false, ignoreRules)) {
                        return FileVisitResult.CONTINUE;
                    }
                    
                    SourceFile sourceFile = FileClassifier.classify(rootPath, file);
                    if (sourceFile != null) {
                        files.add(sourceFile);
                        if (verbose && files.size() % 100 == 0) {
                            System.err.println("Scanning: found " + files.size() + " files...");
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            System.err.println("Error scanning directory: " + e.getMessage());
        }

        files.sort(Comparator.comparing((SourceFile sf) -> {
            if (sf.type() == SourceFile.FileType.SOURCE) return 0;
            if (sf.type() == SourceFile.FileType.TEST) return 1;
            return 2;
        }).thenComparing(sf -> sf.path().toString()));

        return files;
    }

    private boolean isIgnored(Path path, boolean isDir, List<GitIgnoreRule> rules) {
        Path relative = rootPath.relativize(path);
        String relativeStr = relative.toString().replace("\\", "/");
        if (isDir && !relativeStr.endsWith("/")) {
            relativeStr += "/";
        }

        boolean ignored = false;
        for (GitIgnoreRule rule : rules) {
            if (rule.matches(relativeStr)) {
                ignored = !rule.isNegation();
            }
        }
        return ignored;
    }

    private List<GitIgnoreRule> loadGitIgnore() {
        Path gitignorePath = rootPath.resolve(".gitignore");
        List<GitIgnoreRule> rules = new ArrayList<>();
        if (Files.exists(gitignorePath)) {
            try {
                List<String> lines = Files.readAllLines(gitignorePath);
                for (String line : lines) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) continue;
                    rules.add(new GitIgnoreRule(line));
                }
            } catch (IOException e) {
                System.err.println("Error reading .gitignore: " + e.getMessage());
            }
        }
        return rules;
    }

    private static class GitIgnoreRule {
        private final boolean negation;
        private final Pattern pattern;
        
        public GitIgnoreRule(String patternStr) {
            if (patternStr.startsWith("!")) {
                negation = true;
                patternStr = patternStr.substring(1);
            } else {
                negation = false;
            }
            
            // Basic glob to regex conversion
            String regex = patternStr
                .replace(".", "\\.")
                .replace("*", ".*")
                .replace("?", ".");
                
            if (regex.startsWith("/")) {
                regex = "^" + regex.substring(1);
            } else {
                regex = "(^|/)" + regex;
            }
            
            if (regex.endsWith("/")) {
                regex = regex + ".*";
            } else {
                regex = regex + "(/.*)?$";
            }
            
            this.pattern = Pattern.compile(regex);
        }
        
        public boolean isNegation() { return negation; }
        
        public boolean matches(String path) {
            return pattern.matcher(path).find();
        }
    }
}
