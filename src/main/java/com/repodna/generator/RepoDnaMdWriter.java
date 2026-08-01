package com.repodna.generator;

import com.repodna.dna.DnaProfile;
import com.repodna.dna.DnaCategory;
import com.repodna.discovery.model.DiscoveredPattern;
import com.repodna.discovery.model.PatternEvidence;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class RepoDnaMdWriter {
    private final Path outputDir;

    public RepoDnaMdWriter(Path outputDir) {
        this.outputDir = outputDir;
    }

    public void write(DnaProfile dna) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("# Repository Engineering DNA Profile (REPO_DNA.md)\n\n");
        sb.append("This document outlines the engineering patterns discovered in the repository.\n\n");
        
        sb.append("## Summary\n");
        sb.append("- **Overall Confidence**: ").append(Math.round(dna.overallConfidence() * 100)).append("%\n");
        sb.append("- **Total Discovered Patterns**: ").append(dna.totalPatterns()).append("\n\n");

        for (Map.Entry<DnaCategory, List<DiscoveredPattern>> entry : dna.patternsByCategory().entrySet()) {
            sb.append("## ").append(entry.getKey().displayName()).append("\n\n");
            for (DiscoveredPattern pattern : entry.getValue()) {
                sb.append("### ").append(pattern.description()).append("\n");
                sb.append("- **Confidence**: ").append(Math.round(pattern.confidence() * 100)).append("%\n");
                sb.append("- **Occurrences**: ").append(pattern.occurrences()).append("/").append(pattern.totalOpportunities()).append("\n");
                sb.append("- **Reasoning**: ").append(pattern.reasoning()).append("\n\n");
                
                if (!pattern.evidence().isEmpty()) {
                    sb.append("**Evidence**:\n");
                    for (int i = 0; i < Math.min(3, pattern.evidence().size()); i++) {
                        PatternEvidence ev = pattern.evidence().get(i);
                        sb.append("- `").append(ev.filePath()).append(":L").append(ev.lineNumber()).append("`: `").append(ev.snippet()).append("` (").append(ev.explanation()).append(")\n");
                    }
                    sb.append("\n");
                }
            }
        }
        
        Files.writeString(outputDir.resolve("REPO_DNA.md"), sb.toString());
    }
}
