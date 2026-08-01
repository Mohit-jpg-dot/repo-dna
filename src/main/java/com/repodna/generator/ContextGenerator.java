package com.repodna.generator;

import com.repodna.dna.DnaProfile;
import com.repodna.health.model.HealthReport;
import com.repodna.rules.model.EngineeringRule;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class ContextGenerator {
    private final Path outputDir;

    public ContextGenerator(Path outputDir) {
        this.outputDir = outputDir;
    }

    /**
     * Generates all AI context files: AGENTS.md, REPO_DNA.md, PROJECT_RULES.md, ARCHITECTURE.md.
     */
    public void generateAll(DnaProfile dna, HealthReport health, List<EngineeringRule> rules) throws IOException {
        new AgentsMdWriter(outputDir).write(dna, health, rules);
        new RepoDnaMdWriter(outputDir).write(dna);
        new ProjectRulesMdWriter(outputDir).write(rules);
        new ArchitectureMdWriter(outputDir).write(dna, health);
    }
}
