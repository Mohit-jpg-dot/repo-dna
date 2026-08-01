package com.repodna.commands;

import com.repodna.dna.DnaEngine;
import com.repodna.dna.DnaProfile;
import com.repodna.discovery.model.Pattern;
import com.repodna.discovery.model.PatternEvidence;
import com.repodna.discovery.model.PatternViolation;
import com.repodna.util.DirectoryValidator;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.nio.file.Path;
import java.util.concurrent.Callable;

/**
 * Command to explain a discovered rule or pattern with evidence.
 */
@Command(
    name = "explain",
    description = "Explain a discovered rule or pattern with evidence",
    mixinStandardHelpOptions = true
)
public class ExplainCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "The rule/pattern ID to explain (e.g., naming-suffix-service, spring-constructor-injection)")
    private String ruleId;

    @Option(names = {"-d", "--dir"}, description = "The target directory to check", defaultValue = ".")
    private Path dir = Path.of(".");

    @Option(names = {"--verbose"}, description = "Show verbose explanation and all evidence")
    private boolean verbose = false;

    @Override
    public Integer call() {
        Path targetDir = dir.toAbsolutePath().normalize();
        DirectoryValidator.validate(targetDir);

        System.out.println("Running analysis on " + targetDir.getFileName() + " to lookup pattern...");
        DnaProfile profile = DnaEngine.analyze(targetDir);

        Pattern targetPattern = null;
        for (Pattern p : profile.patterns()) {
            if (p.id().equalsIgnoreCase(ruleId)) {
                targetPattern = p;
                break;
            }
        }

        if (targetPattern == null) {
            System.err.printf("Error: Pattern/Rule ID '%s' not found.\n", ruleId);
            System.out.println("Available IDs:");
            for (Pattern p : profile.patterns()) {
                System.out.println("  - " + p.id());
            }
            return 1;
        }

        System.out.println("\nPattern Explanation: " + targetPattern.name());
        System.out.println("=================================================");
        System.out.println("ID:            " + targetPattern.id());
        System.out.println("Category:      " + targetPattern.category());
        System.out.println("Description:   " + targetPattern.description());
        System.out.printf("Confidence:    %.0f%% (Score: %.2f)\n", targetPattern.confidence().score() * 100, targetPattern.confidence().score());
        System.out.println("Support:       " + targetPattern.confidence().supportCount() + " conformant files");
        System.out.println("Violations:    " + targetPattern.confidence().violationCount() + " anomalies detected");
        System.out.printf("Coverage:      %.0f%% of candidates\n", targetPattern.confidence().coverage() * 100);
        System.out.println("Stability:     " + targetPattern.history().stability() + " (Age: " + targetPattern.history().ageCommits() + " commits)");
        System.out.println("=================================================");

        if (!targetPattern.evidenceList().isEmpty()) {
            System.out.println("\nSupporting Evidence / Examples:");
            int count = 0;
            for (PatternEvidence ev : targetPattern.evidenceList()) {
                System.out.printf("  [✓] %s: %s\n", ev.nodeId(), ev.description());
                if (!verbose && ++count >= 5) {
                    System.out.printf("  ... and %d more. Use --verbose to see all.\n", targetPattern.evidenceList().size() - 5);
                    break;
                }
            }
        }

        if (!targetPattern.violationsList().isEmpty()) {
            System.out.println("\nViolations / Outliers:");
            int count = 0;
            for (PatternViolation v : targetPattern.violationsList()) {
                System.out.printf("  [✗] %s: %s\n", v.nodeId(), v.description());
                if (!verbose && ++count >= 5) {
                    System.out.printf("  ... and %d more. Use --verbose to see all.\n", targetPattern.violationsList().size() - 5);
                    break;
                }
            }
        }

        return 0;
    }
}
