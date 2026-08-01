package com.repodna.scanner.detector;

import com.repodna.scanner.model.LanguageInfo;

import java.nio.file.Path;
import java.util.*;

/**
 * Detects programming languages present in the repository based on file extensions.
 */
public class LanguageDetector {
    private static final Map<String, String> EXTENSION_TO_LANG = Map.ofEntries(
        Map.entry("java", "Java"),
        Map.entry("kt", "Kotlin"),
        Map.entry("kts", "Kotlin"),
        Map.entry("py", "Python"),
        Map.entry("ts", "TypeScript"),
        Map.entry("js", "JavaScript"),
        Map.entry("rs", "Rust"),
        Map.entry("go", "Go"),
        Map.entry("cs", "C#"),
        Map.entry("cpp", "C++"),
        Map.entry("c", "C"),
        Map.entry("h", "C/C++ Header"),
        Map.entry("swift", "Swift"),
        Map.entry("rb", "Ruby"),
        Map.entry("php", "PHP")
    );

    /**
     * Aggregates languages from file list and returns stats sorted by file count descending.
     */
    public static List<LanguageInfo> detect(List<Path> files) {
        Map<String, Integer> counts = new HashMap<>();
        int totalCodeFiles = 0;

        for (Path file : files) {
            String fileName = file.getFileName().toString();
            int dotIndex = fileName.lastIndexOf('.');
            if (dotIndex != -1 && dotIndex < fileName.length() - 1) {
                String ext = fileName.substring(dotIndex + 1).toLowerCase();
                String lang = EXTENSION_TO_LANG.get(ext);
                if (lang != null) {
                    counts.put(lang, counts.getOrDefault(lang, 0) + 1);
                    totalCodeFiles++;
                }
            }
        }

        List<LanguageInfo> result = new ArrayList<>();
        if (totalCodeFiles == 0) {
            return result;
        }

        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            double percentage = (entry.getValue() * 100.0) / totalCodeFiles;
            result.add(new LanguageInfo(entry.getKey(), entry.getValue(), Math.round(percentage * 100.0) / 100.0));
        }

        result.sort((a, b) -> Integer.compare(b.fileCount(), a.fileCount()));
        return result;
    }
}
