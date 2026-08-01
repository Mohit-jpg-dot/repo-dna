package com.repodna.cli;

import com.repodna.RepoDnaCli;
import com.repodna.storage.DatabaseManager;
import com.repodna.storage.AnalysisStore;
import com.repodna.util.Console;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;

/**
 * Evaluates repository health based on engineering rules.
 */
@Command(
    name = "health",
    description = "Evaluate repository health against best practices",
    mixinStandardHelpOptions = true
)
public class HealthCommand implements Callable<Integer> {

    @ParentCommand
    private RepoDnaCli parent;

    @Option(names = {"--dimension"}, description = "Filter by dimension: Architecture, Naming, Security, Testing, Maintainability, AI Readiness")
    private String dimension;

    @Override
    public Integer call() {
        Path repoDir = parent.getRepoDir().toAbsolutePath().normalize();
        Path repodnaDir = repoDir.resolve(".repodna");
        
        Console.header("Repository Health Status");
        
        if (!Files.exists(repodnaDir)) {
            Console.error("RepoDNA is not initialized. Please run 'repo-dna init' first.");
            return 1;
        }

        try (DatabaseManager dbManager = new DatabaseManager(repodnaDir.resolve("repodna.db"))) {
            AnalysisStore store = new AnalysisStore(dbManager);
            Optional<Long> lastRunId = store.getLastAnalysisRun();
            if (lastRunId.isEmpty()) {
                Console.warning("No analysis runs found. Please run 'repo-dna analyze' first.");
                return 0;
            }
            
            long runId = lastRunId.get();
            Map<String, Integer> scores = store.getHealthScores(runId);
            
            if (scores.isEmpty()) {
                Console.warning("No health scores found for the last analysis run.");
                return 0;
            }
            
            if (dimension != null) {
                String cleanDim = dimension.trim();
                Optional<Map.Entry<String, Integer>> scoreMatch = scores.entrySet().stream()
                    .filter(e -> e.getKey().equalsIgnoreCase(cleanDim))
                    .findFirst();
                if (scoreMatch.isPresent()) {
                    Console.scoreBar(scoreMatch.get().getKey(), scoreMatch.get().getValue());
                } else {
                    Console.error("Dimension '" + dimension + "' not found. Available: " + scores.keySet());
                }
            } else {
                int overall = (int) scores.values().stream().mapToInt(Integer::intValue).average().orElse(50);
                Console.scoreBar("Overall Health", overall);
                Console.blank();
                Console.println("Dimension Breakdown:");
                for (Map.Entry<String, Integer> entry : scores.entrySet()) {
                    Console.scoreBar("  " + String.format("%-18s", entry.getKey()), entry.getValue());
                }
            }
            
            return 0;
        } catch (Exception e) {
            Console.error("Failed to read health scores: " + e.getMessage());
            return 1;
        }
    }
}
