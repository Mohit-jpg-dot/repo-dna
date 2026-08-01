package com.repodna.cli;

import com.repodna.RepoDnaCli;
import com.repodna.scanner.ProjectDetector;
import com.repodna.model.ProjectInfo;
import com.repodna.storage.DatabaseManager;
import com.repodna.util.Console;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

/**
 * Initializes RepoDNA for the current repository.
 */
@Command(
    name = "init",
    description = "Initialize RepoDNA for this repository",
    mixinStandardHelpOptions = true
)
public class InitCommand implements Callable<Integer> {

    @ParentCommand
    private RepoDnaCli parent;

    @Option(names = {"--force"}, description = "Re-initialize even if already initialized")
    private boolean force;

    @Override
    public Integer call() {
        Path repoDir = parent.getRepoDir().toAbsolutePath().normalize();
        Path repodnaDir = repoDir.resolve(".repodna");
        
        Console.header("Initializing RepoDNA");
        Console.info("Working directory: " + repoDir);
        
        if (Files.exists(repodnaDir) && !force) {
            Console.warning("RepoDNA is already initialized in this repository.");
            Console.info("Use 'repo-dna analyze' to run analysis, or 'repo-dna init --force' to re-initialize.");
            return 0;
        }
        
        try {
            if (!Files.exists(repodnaDir)) {
                Files.createDirectories(repodnaDir);
                Console.success("Created .repodna/ directory.");
            }
            
            // Detect project type
            Console.info("Detecting project layout and build tool...");
            ProjectInfo info = ProjectDetector.detect(repoDir);
            Console.success("Project detected: " + info.name());
            Console.info("Build tool: " + info.buildTool() + " | Framework: " + info.framework());
            
            // Initialize SQLite DB
            Path dbPath = repodnaDir.resolve("repodna.db");
            Console.info("Initializing SQLite database at: " + dbPath);
            try (DatabaseManager dbManager = new DatabaseManager(dbPath)) {
                dbManager.initialize();
                Console.success("SQLite database initialized successfully.");
            }
            
            Console.blank();
            Console.success("RepoDNA initialized successfully for " + info.name() + "!");
            Console.info("Next step: Run 'repo-dna analyze' to discover engineering DNA.");
            
            return 0;
        } catch (Exception e) {
            Console.error("Initialization failed: " + e.getMessage());
            if (parent.isVerbose()) {
                e.printStackTrace();
            }
            return 1;
        }
    }
}
