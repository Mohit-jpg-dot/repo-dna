package com.repodna.cli;

import com.repodna.RepoDnaCli;
import com.repodna.rules.model.EngineeringRule;
import com.repodna.rules.model.RuleViolation;
import com.repodna.discovery.model.PatternEvidence;
import com.repodna.storage.DatabaseManager;
import com.repodna.storage.AnalysisStore;
import com.repodna.util.Console;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;

/**
 * Explains a discovered rule with evidence.
 */
@Command(
    name = "explain",
    description = "Explain a discovered rule or pattern with evidence",
    mixinStandardHelpOptions = true
)
public class ExplainCommand implements Callable<Integer> {

    @ParentCommand
    private RepoDnaCli parent;

    @Parameters(index = "0", description = "The rule ID to explain")
    private String ruleId;

    @Override
    public Integer call() {
        Path repoDir = parent.getRepoDir().toAbsolutePath().normalize();
        Path repodnaDir = repoDir.resolve(".repodna");
        
        Console.header("Rule Explanation: " + ruleId);
        
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
            
            Optional<EngineeringRule> match = rules.stream()
                .filter(r -> r.id().equalsIgnoreCase(ruleId))
                .findFirst();
                
            if (match.isEmpty()) {
                Console.error("Rule with ID '" + ruleId + "' not found.");
                return 1;
            }
            
            EngineeringRule rule = match.get();
            Console.println(Console.isColorsEnabled() ? "\u001B[1mDescription:\u001B[0m " + rule.description() : "Description: " + rule.description());
            Console.println(Console.isColorsEnabled() ? "\u001B[1mCategory:\u001B[0m    " + rule.category() : "Category:    " + rule.category());
            Console.println(Console.isColorsEnabled() ? "\u001B[1mConfidence:\u001B[0m  " + Math.round(rule.confidence() * 100) + "%" : "Confidence:  " + Math.round(rule.confidence() * 100) + "%");
            Console.blank();
            
            Console.println(Console.isColorsEnabled() ? "\u001B[1mRationale:\u001B[0m" : "Rationale:");
            Console.println("  " + rule.rationale());
            Console.blank();
            
            if (!rule.evidence().isEmpty()) {
                Console.println(Console.isColorsEnabled() ? "\u001B[1mSupporting Evidence:\u001B[0m" : "Supporting Evidence:");
                for (int i = 0; i < Math.min(5, rule.evidence().size()); i++) {
                    PatternEvidence ev = rule.evidence().get(i);
                    Console.println(String.format("  ✓ %s:L%d -> %s", ev.filePath(), ev.lineNumber(), ev.snippet()));
                }
                if (rule.evidence().size() > 5) {
                    Console.println("    ... and " + (rule.evidence().size() - 5) + " more supporting occurrences.");
                }
                Console.blank();
            }
            
            if (!rule.violations().isEmpty()) {
                Console.println(Console.isColorsEnabled() ? "\u001B[1;31mActive Violations (" + rule.violations().size() + "):\u001B[0m" : "Active Violations (" + rule.violations().size() + "):");
                for (RuleViolation violation : rule.violations()) {
                    Console.println(String.format("  ✗ %s:L%d", violation.filePath(), violation.lineNumber()));
                    Console.println("    * Issue:  " + violation.description());
                    Console.println("    * Fix:    " + violation.suggestion());
                }
            } else {
                Console.success("Perfect adherence! No active violations of this rule detected in the codebase.");
            }
            
            return 0;
        } catch (Exception e) {
            Console.error("Failed to explain rule: " + e.getMessage());
            return 1;
        }
    }
}
