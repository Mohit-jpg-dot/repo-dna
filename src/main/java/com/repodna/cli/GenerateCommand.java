package com.repodna.cli;

import com.repodna.RepoDnaCli;
import com.repodna.discovery.model.DiscoveredPattern;
import com.repodna.dna.DnaEngine;
import com.repodna.dna.DnaProfile;
import com.repodna.health.model.HealthReport;
import com.repodna.health.model.HealthScore;
import com.repodna.rules.model.EngineeringRule;
import com.repodna.generator.ContextGenerator;
import com.repodna.storage.DatabaseManager;
import com.repodna.storage.AnalysisStore;
import com.repodna.util.Console;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;

/**
 * Generates AI context files.
 */
@Command(
    name = "generate",
    description = "Generate AI context files (AGENTS.md, REPO_DNA.md, etc.)",
    mixinStandardHelpOptions = true
)
public class GenerateCommand implements Callable<Integer> {

    @ParentCommand
    private RepoDnaCli parent;

    @Option(names = {"--output-dir"}, description = "Where to write files (default: repo root)")
    private Path outputDir;

    @Option(names = {"--files"}, description = "Which files to generate: agents, dna, rules, architecture or 'all'", defaultValue = "all")
    private String files;

    @Override
    public Integer call() {
        Path repoDir = parent.getRepoDir().toAbsolutePath().normalize();
        Path repodnaDir = repoDir.resolve(".repodna");
        Path targetDir = outputDir != null ? outputDir.toAbsolutePath().normalize() : repoDir;
        
        Console.header("Generating AI Context Files");
        
        if (!Files.exists(repodnaDir)) {
            Console.error("RepoDNA is not initialized. Please run 'repo-dna init' first.");
            return 1;
        }

        try (DatabaseManager dbManager = new DatabaseManager(repodnaDir.resolve("repodna.db"))) {
            AnalysisStore store = new AnalysisStore(dbManager);
            Optional<Long> lastRunId = store.getLastAnalysisRun();
            if (lastRunId.isEmpty()) {
                Console.warning("No analysis runs found. Please run 'repo-dna analyze' first to populate data.");
                return 0;
            }
            
            long runId = lastRunId.get();
            List<DiscoveredPattern> patterns = store.getPatterns(runId);
            List<EngineeringRule> rules = store.getRules(runId);
            Map<String, Integer> scoreMap = store.getHealthScores(runId);
            
            if (patterns.isEmpty() || rules.isEmpty()) {
                Console.warning("Missing pattern or rule data in database. Re-run 'repo-dna analyze'.");
                return 0;
            }
            
            // Re-evaluate health and DNA profiles
            DnaEngine dnaEngine = new DnaEngine();
            DnaProfile dnaProfile = dnaEngine.buildProfile("Project", patterns);
            
            List<HealthScore> healthScores = new ArrayList<>();
            for (Map.Entry<String, Integer> entry : scoreMap.entrySet()) {
                healthScores.add(new HealthScore(entry.getKey(), entry.getValue(), HealthScore.gradeFor(entry.getValue()), List.of(), List.of(), List.of()));
            }
            int overall = (int) scoreMap.values().stream().mapToInt(Integer::intValue).average().orElse(50);
            int aiReadiness = scoreMap.getOrDefault("AI Readiness", 50);
            
            HealthReport healthReport = new HealthReport(overall, aiReadiness, 0.0, healthScores, rules.size(), 0);
            
            Console.info("Writing context files to: " + targetDir);
            ContextGenerator generator = new ContextGenerator(targetDir);
            generator.generateAll(dnaProfile, healthReport, rules);
            
            Console.success("✓ AI context files generated successfully.");
            return 0;
        } catch (Exception e) {
            Console.error("Failed to generate context files: " + e.getMessage());
            if (parent.isVerbose()) {
                e.printStackTrace();
            }
            return 1;
        }
    }
}
