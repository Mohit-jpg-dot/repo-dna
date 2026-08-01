package com.repodna;

import com.repodna.cli.*;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ScopeType;

import java.nio.file.Path;
import java.util.concurrent.Callable;

/**
 * Main entry point for the RepoDNA CLI.
 */
@Command(
    name = "repo-dna",
    mixinStandardHelpOptions = true,
    version = "RepoDNA 1.0.0",
    description = "Engineering Intelligence Layer — teaches AI how your team builds software.",
    subcommands = {
        InitCommand.class,
        AnalyzeCommand.class,
        HealthCommand.class,
        ExplainCommand.class,
        RulesCommand.class,
        GenerateCommand.class,
        UpdateCommand.class,
        DoctorCommand.class,
        CommandLine.HelpCommand.class
    }
)
public class RepoDnaCli implements Callable<Integer> {
    
    @Option(names = {"-v", "--verbose"}, description = "Enable verbose output", scope = ScopeType.INHERIT)
    private boolean verbose;
    
    @Option(names = {"--no-color"}, description = "Disable colored output", scope = ScopeType.INHERIT)
    private boolean noColor;
    
    @Option(names = {"-d", "--dir"}, description = "Repository directory (default: current directory)", scope = ScopeType.INHERIT)
    private Path repoDir = Path.of(".");
    
    public boolean isVerbose() {
        return verbose;
    }
    
    public boolean isNoColor() {
        return noColor;
    }
    
    public Path getRepoDir() {
        return repoDir;
    }

    @Override
    public Integer call() {
        CommandLine.usage(this, System.out);
        return 0;
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new RepoDnaCli()).execute(args);
        System.exit(exitCode);
    }
}
