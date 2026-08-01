package com.repodna.cli;

import com.repodna.RepoDnaCli;
import com.repodna.rules.model.EngineeringRule;
import com.repodna.storage.DatabaseManager;
import com.repodna.storage.AnalysisStore;
import com.repodna.util.Console;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;

/**
 * Lists all discovered engineering rules.
 */
@Command(
    name = "rules",
    description = "List all discovered engineering rules",
    mixinStandardHelpOptions = true
)
public class RulesCommand implements Callable<Integer> {

    @ParentCommand
    private RepoDnaCli parent;

    @Option(names = {"--category"}, description = "Filter by category")
    private String category;

    @Option(names = {"--min-confidence"}, description = "Minimum confidence threshold", defaultValue = "0.7")
    private double minConfidence;

    @Option(names = {"--format"}, description = "Output format: text/json", defaultValue = "text")
    private String format;

    @Override
    public Integer call() {
        Path repoDir = parent.getRepoDir().toAbsolutePath().normalize();
        Path repodnaDir = repoDir.resolve(".repodna");
        
        Console.header("Discovered Engineering Rules");
        
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
            List<EngineeringRule> rules = store.getRules(runId);
            
            if (rules.isEmpty()) {
                Console.warning("No rules discovered in the last analysis run.");
                return 0;
            }
            
            List<EngineeringRule> filtered = rules.stream()
                .filter(r -> r.confidence() >= minConfidence)
                .filter(r -> category == null || r.category().equalsIgnoreCase(category))
                .toList();
                
            if (filtered.isEmpty()) {
                Console.warning("No rules match the specified filters.");
                return 0;
            }
            
            if ("json".equalsIgnoreCase(format)) {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(filtered));
            } else {
                String[] headers = {"Rule ID", "Category", "Confidence", "Violations", "Description"};
                List<String[]> rows = new ArrayList<>();
                for (EngineeringRule r : filtered) {
                    rows.add(new String[]{
                        r.id(),
                        r.category(),
                        String.format("%d%%", Math.round(r.confidence() * 100)),
                        String.valueOf(r.violations().size()),
                        r.description()
                    });
                }
                Console.table(headers, rows);
                Console.blank();
                Console.info("Use 'repo-dna explain [rule-id]' to see full evidence and suggestions for a rule.");
            }
            
            return 0;
        } catch (Exception e) {
            Console.error("Failed to read rules: " + e.getMessage());
            return 1;
        }
    }
}
