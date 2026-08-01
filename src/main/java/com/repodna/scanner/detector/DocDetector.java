package com.repodna.scanner.detector;

import com.repodna.scanner.model.DocumentationInfo;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Detects presence of standard repository documentation files.
 */
public class DocDetector {
    private static final Set<String> DOC_BASENAMES = Set.of(
        "README", "CONTRIBUTING", "LICENSE", "CHANGELOG", "SECURITY", "CODE_OF_CONDUCT"
    );

    /**
     * Scans for documentation base names (e.g. README, LICENSE) with any extension.
     */
    public static DocumentationInfo detect(List<Path> files) {
        List<String> presentDocs = new ArrayList<>();
        for (Path file : files) {
            String fileName = file.getFileName().toString();
            int dotIndex = fileName.lastIndexOf('.');
            String base = dotIndex == -1 ? fileName : fileName.substring(0, dotIndex);

            if (DOC_BASENAMES.contains(base.toUpperCase())) {
                presentDocs.add(fileName);
            }
        }
        return new DocumentationInfo(presentDocs);
    }
}
