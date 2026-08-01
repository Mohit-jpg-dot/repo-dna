package com.repodna.cli;

import com.repodna.RepoDnaCli;
import com.repodna.scanner.*;
import com.repodna.parser.JavaAstParser;
import com.repodna.parser.model.ParsedFile;
import com.repodna.model.SourceFile;
import com.repodna.model.ProjectInfo;
import com.repodna.graph.*;
import com.repodna.discovery.*;
import com.repodna.discovery.model.DiscoveredPattern;
import com.repodna.dna.*;
import com.repodna.health.*;
import com.repodna.health.model.*;
import com.repodna.rules.*;
import com.repodna.rules.model.EngineeringRule;
import com.repodna.generator.ContextGenerator;
import com.repodna.storage.*;
import com.repodna.util.*;

import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Callable;

/**
 * Main command to run the full analysis pipeline.
 */
@Command(
    name = "analyze",
    description = "Analyze repository and discover engineering DNA",
    mixinStandardHelpOptions = true
)
public class AnalyzeCommand implements Callable<Integer> {

    @ParentCommand
    private RepoDnaCli parent;

    @Option(names = {"--full"}, description = "Full analysis vs incremental")
    private boolean full;

    @Option(names = {"--output"}, description = "Output format: text/json", defaultValue = "text")
    private String output;

    @Override
    public Integer call() {
        Path repoDir = parent.getRepoDir().toAbsolutePath().normalize();
        Path repodnaDir = repoDir.resolve(".repodna");
        
        Console.header("RepoDNA Analysis Pipeline");
        Console.info("Working directory: " + repoDir);
        
        if (!Files.exists(repodnaDir)) {
            Console.error("RepoDNA is not initialized in this repository.");
            Console.info("Please run 'repo-dna init' first.");
            return 1;
        }

        ProgressReporter progress = new ProgressReporter(10);
        
        try {
            // Step 1: Detect Project Info
            progress.startStep("Detecting project layout");
            ProjectInfo projectInfo = ProjectDetector.detect(repoDir);
            progress.completeStep();

            // Step 2: Scan Repository Files
            progress.startStep("Scanning repository files");
            RepositoryScanner scanner = new RepositoryScanner(repoDir, parent.isVerbose());
            List<SourceFile> sourceFiles = scanner.scan();
            progress.completeStep();

            // Step 3: Parse AST (Java files only)
            progress.startStep("Parsing AST via Tree-sitter");
            List<ParsedFile> parsedFiles = new ArrayList<>();
            try (JavaAstParser astParser = new JavaAstParser()) {
                for (SourceFile sf : sourceFiles) {
                    if (sf.type() == SourceFile.FileType.SOURCE || sf.type() == SourceFile.FileType.TEST) {
                        if ("java".equalsIgnoreCase(sf.language())) {
                            try {
                                ParsedFile parsed = astParser.parse(repoDir.resolve(sf.path()));
                                parsedFiles.add(parsed);
                            } catch (Exception e) {
                                if (parent.isVerbose()) {
                                    System.err.println("Warning: failed to parse " + sf.path() + ": " + e.getMessage());
                                }
                            }
                        }
                    }
                }
            }
            progress.completeStep();

            // Step 4: Build Dependency Graph
            progress.startStep("Building Dependency Graph");
            DependencyGraphBuilder graphBuilder = new DependencyGraphBuilder(parsedFiles);
            DefaultDirectedGraph<String, DefaultEdge> classGraph = graphBuilder.buildClassGraph();
            DefaultDirectedGraph<String, DefaultEdge> packageGraph = graphBuilder.buildPackageGraph();
            progress.completeStep();

            // Step 5: Detect Architectural Layers
            progress.startStep("Analyzing Architectural Layers");
            List<LayerDetector.LayerAssignment> layerAssignments = LayerDetector.assignLayers(parsedFiles);
            Map<String, LayerDetector.Layer> layerMap = new HashMap<>();
            for (LayerDetector.LayerAssignment la : layerAssignments) {
                layerMap.put(la.className(), la.layer());
            }
            List<LayerDetector.LayerViolation> layerViolations = LayerDetector.detectViolations(layerMap, classGraph);
            progress.completeStep();

            // Step 6: Discover Patterns
            progress.startStep("Discovering Engineering Patterns");
            AnalysisContext context = new AnalysisContext(
                projectInfo, 
                new ArrayList<>(sourceFiles), 
                parsedFiles, 
                classGraph, 
                packageGraph, 
                layerMap, 
                layerViolations
            );
            PatternDiscoveryEngine discoveryEngine = new PatternDiscoveryEngine();
            List<DiscoveredPattern> discoveredPatterns = discoveryEngine.discoverPatterns(context);
            progress.completeStep();

            // Step 7: Build DNA Profile
            progress.startStep("Assembling DNA Profile");
            DnaEngine dnaEngine = new DnaEngine();
            DnaProfile dnaProfile = dnaEngine.buildProfile(projectInfo.name(), discoveredPatterns);
            progress.completeStep();

            // Step 8: Generate and Evaluate Rules
            progress.startStep("Generating Engineering Rules");
            RuleGenerator ruleGenerator = new RuleGenerator();
            List<EngineeringRule> generatedRules = ruleGenerator.generateRules(discoveredPatterns, 0.7);
            
            RuleEngine ruleEngine = new RuleEngine();
            List<EngineeringRule> evaluatedRules = ruleEngine.evaluateRules(context, generatedRules);
            progress.completeStep();

            // Step 9: Evaluate Health Report
            progress.startStep("Evaluating Codebase Health");
            HealthEngine healthEngine = new HealthEngine();
            HealthReport healthReport = healthEngine.evaluate(context, discoveredPatterns, evaluatedRules.size());
            progress.completeStep();

            // Step 10: Generate AI Context Documents & Save to DB
            progress.startStep("Writing AI Context Files & Saving Database");
            ContextGenerator generator = new ContextGenerator(repoDir);
            generator.generateAll(dnaProfile, healthReport, evaluatedRules);
            
            try (DatabaseManager dbManager = new DatabaseManager(repodnaDir.resolve("repodna.db"))) {
                AnalysisStore store = new AnalysisStore(dbManager);
                long runId = store.saveAnalysisRun(projectInfo);
                store.saveSourceFiles(sourceFiles, runId);
                
                Map<String, Integer> scoreMap = new HashMap<>();
                for (HealthScore hs : healthReport.dimensionScores()) {
                    scoreMap.put(hs.dimension(), hs.score());
                }
                store.saveHealthScores(scoreMap, runId);
                store.savePatterns(discoveredPatterns, runId);
                store.saveRules(evaluatedRules, runId);
            }
            progress.completeStep();
            
            progress.finish();

            // Render Wow Moment Output
            renderWowMoment(projectInfo, healthReport, dnaProfile, evaluatedRules);

            return 0;
        } catch (Exception e) {
            Console.blank();
            Console.error("Analysis failed: " + e.getMessage());
            if (parent.isVerbose()) {
                e.printStackTrace();
            }
            return 1;
        }
    }

    private void renderWowMoment(ProjectInfo projectInfo, HealthReport health, DnaProfile dna, List<EngineeringRule> rules) {
        Console.blank();
        Console.header("Repository Engineering DNA Discovered!");
        
        List<String> boxLines = new ArrayList<>();
        boxLines.add("Project Name:   " + projectInfo.name());
        boxLines.add("Build Tool:     " + projectInfo.buildTool());
        boxLines.add("Framework:      " + projectInfo.framework());
        boxLines.add("Java Version:   " + projectInfo.javaVersion());
        boxLines.add("Patterns Found: " + dna.totalPatterns());
        boxLines.add("Active Rules:   " + rules.size());
        
        Console.box("Analysis Summary", boxLines);
        Console.blank();
        
        Console.println(Console.isColorsEnabled() ? "\u001B[1mOVERALL HEALTH SCORE\u001B[0m" : "OVERALL HEALTH SCORE");
        Console.scoreBar("Codebase Quality", health.overallScore());
        Console.blank();
        
        Console.println(Console.isColorsEnabled() ? "\u001B[1mHEALTH BREAKDOWN\u001B[0m" : "HEALTH BREAKDOWN");
        for (HealthScore hs : health.dimensionScores()) {
            Console.scoreBar(String.format("%-18s", hs.dimension()), hs.score());
        }
        Console.blank();
        
        if (!dna.teamSignatures().isEmpty()) {
            Console.println(Console.isColorsEnabled() ? "\u001B[1m✓ MAIN TEAM SIGNATURES (High Adherence)\u001B[0m" : "✓ MAIN TEAM SIGNATURES (High Adherence)");
            for (String sig : dna.teamSignatures()) {
                Console.success(sig);
            }
            Console.blank();
        }

        int violationsCount = rules.stream().mapToInt(r -> r.violations().size()).sum();
        if (violationsCount > 0) {
            Console.println(Console.isColorsEnabled() ? "\u001B[1;31m⚠ ENCOURAGED RULES & VIOLATIONS\u001B[0m" : "⚠ ENCOURAGED RULES & VIOLATIONS");
            for (EngineeringRule r : rules) {
                if (!r.violations().isEmpty()) {
                    Console.warning(r.description() + " (Confidence: " + Math.round(r.confidence() * 100) + "%)");
                    for (int i = 0; i < Math.min(2, r.violations().size()); i++) {
                        var v = r.violations().get(i);
                        Console.println("   - Violation in " + v.filePath() + ":L" + v.lineNumber());
                        Console.println("     * Issue: " + v.description());
                        Console.println("     * Fix: " + v.suggestion());
                    }
                    if (r.violations().size() > 2) {
                        Console.println("   - ... and " + (r.violations().size() - 2) + " more violations.");
                    }
                }
            }
            Console.blank();
        } else {
            Console.success("100% adherence to all discovered rules! Perfect alignment.");
            Console.blank();
        }
        
        Console.divider();
        Console.success("AI Context Files updated at project root:");
        Console.info("- AGENTS.md (Instructions for Cursor/Copilot/Claude Code)");
        Console.info("- REPO_DNA.md (Extracted engineering patterns)");
        Console.info("- PROJECT_RULES.md (Rules with example violations)");
        Console.info("- ARCHITECTURE.md (Layer diagrams & health scores)");
        Console.divider();
    }
}
