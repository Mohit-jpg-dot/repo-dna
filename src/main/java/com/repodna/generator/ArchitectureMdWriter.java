package com.repodna.generator;

import com.repodna.dna.DnaProfile;
import com.repodna.dna.DnaCategory;
import com.repodna.health.model.HealthReport;
import com.repodna.health.model.HealthScore;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ArchitectureMdWriter {
    private final Path outputDir;

    public ArchitectureMdWriter(Path outputDir) {
        this.outputDir = outputDir;
    }

    public void write(DnaProfile dna, HealthReport health) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("# Project Architecture Overview (ARCHITECTURE.md)\n\n");
        
        sb.append("## Architectural Integrity\n");
        sb.append("- **Overall Architecture Grade**: ").append(health.overallGrade()).append(" (Score: ").append(health.overallScore()).append("/100)\n");
        sb.append("- **Architecture Drift**: ").append(String.format("%.1f", health.architectureDrift())).append("%\n\n");

        sb.append("## Health Dimension Scores\n");
        for (HealthScore score : health.dimensionScores()) {
            sb.append("- **").append(score.dimension()).append("**: ").append(score.score()).append("/100 (").append(score.grade()).append(")\n");
            if (!score.strengths().isEmpty()) {
                sb.append("  - *Strengths*: ").append(String.join(", ", score.strengths())).append("\n");
            }
            if (!score.improvements().isEmpty()) {
                sb.append("  - *Improvements*: ").append(String.join(", ", score.improvements())).append("\n");
            }
        }
        sb.append("\n");

        sb.append("## Mermaid Component/Layer Diagram\n\n");
        sb.append("```mermaid\n");
        sb.append("graph TD\n");
        sb.append("    Controller[Controller Layer] --> Service[Service Layer]\n");
        sb.append("    Service --> Repository[Repository Layer]\n");
        sb.append("    Repository --> Database[(SQLite / DB)]\n");
        sb.append("```\n");
        
        Files.writeString(outputDir.resolve("ARCHITECTURE.md"), sb.toString());
    }
}
