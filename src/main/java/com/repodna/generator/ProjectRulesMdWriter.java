package com.repodna.generator;

import com.repodna.rules.model.EngineeringRule;
import com.repodna.rules.model.RuleViolation;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class ProjectRulesMdWriter {
    private final Path outputDir;

    public ProjectRulesMdWriter(Path outputDir) {
        this.outputDir = outputDir;
    }

    public void write(List<EngineeringRule> rules) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("# Project Engineering Rules (PROJECT_RULES.md)\n\n");
        sb.append("This file lists the enforceable engineering rules learned from this repository.\n\n");
        
        for (EngineeringRule rule : rules) {
            sb.append("## Rule: ").append(rule.description()).append("\n");
            sb.append("- **Rule ID**: `").append(rule.id()).append("`\n");
            sb.append("- **Confidence**: ").append(Math.round(rule.confidence() * 100)).append("%\n");
            sb.append("- **Category**: ").append(rule.category()).append("\n");
            sb.append("- **Rationale**: ").append(rule.rationale()).append("\n\n");
            
            if (!rule.violations().isEmpty()) {
                sb.append("### Active Violations (").append(rule.violations().size()).append(")\n");
                for (RuleViolation violation : rule.violations()) {
                    sb.append("- **Location**: `").append(violation.filePath()).append(":L").append(violation.lineNumber()).append("`\n");
                    sb.append("  - *Issue*: ").append(violation.description()).append("\n");
                    sb.append("  - *Suggestion*: ").append(violation.suggestion()).append("\n");
                }
                sb.append("\n");
            } else {
                sb.append("✓ **0 violations detected.** Adherence is 100%.\n\n");
            }
        }
        
        Files.writeString(outputDir.resolve("PROJECT_RULES.md"), sb.toString());
    }
}
