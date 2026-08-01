package com.repodna.generator;

import com.repodna.dna.DnaProfile;
import com.repodna.health.model.HealthReport;
import com.repodna.rules.model.EngineeringRule;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class AgentsMdWriter {
    private final Path outputDir;

    public AgentsMdWriter(Path outputDir) {
        this.outputDir = outputDir;
    }

    public void write(DnaProfile dna, HealthReport health, List<EngineeringRule> rules) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("# AI Agent Coding Instructions (AGENTS.md)\n\n");
        sb.append("> **Important**: This repository follows strict engineering rules. Adhere to these patterns when generating code.\n\n");
        
        sb.append("## Project Context\n");
        sb.append("- **Project Name**: ").append(dna.projectName()).append("\n");
        sb.append("- **Overall Health**: ").append(health.overallScore()).append("/100 (").append(health.overallGrade()).append(")\n");
        sb.append("- **AI Readiness**: ").append(health.aiReadinessScore()).append("/100\n\n");
        
        sb.append("## Core Signatures learned from Repository\n");
        for (String sig : dna.teamSignatures()) {
            sb.append("- ").append(sig).append("\n");
        }
        sb.append("\n");

        sb.append("## DOs and DONTs\n");
        sb.append("### DO\n");
        for (EngineeringRule rule : rules) {
            if (rule.confidence() >= 0.8 && rule.violations().isEmpty()) {
                sb.append("- **").append(rule.category()).append("**: ").append(rule.description()).append("\n");
            }
        }
        sb.append("\n### DONT\n");
        for (EngineeringRule rule : rules) {
            if (!rule.violations().isEmpty()) {
                sb.append("- Avoid violating rule: **").append(rule.description()).append("**\n");
                if (rule.violations().size() > 0) {
                    sb.append("  - *Example violation found in: ").append(rule.violations().get(0).filePath()).append(":L").append(rule.violations().get(0).lineNumber()).append("*\n");
                }
            }
        }
        
        Files.writeString(outputDir.resolve("AGENTS.md"), sb.toString());
    }
}
