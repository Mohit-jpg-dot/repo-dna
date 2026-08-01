package com.repodna.cli;

import com.repodna.commands.*;
import com.repodna.exception.RepoDnaException;
import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

/**
 * Main Picocli entry point for the RepoDNA command line utility.
 */
@Command(
    name = "repo-dna",
    mixinStandardHelpOptions = true,
    version = "1.0.0",
    description = "Engineering Intelligence Layer — teaches AI how your team builds software.",
    subcommands = {
        InitCommand.class,
        AnalyzeCommand.class,
        HealthCommand.class,
        ExplainCommand.class,
        VersionCommand.class
    }
)
public class RepoDnaCli implements Callable<Integer> {

    @Override
    public Integer call() {
        CommandLine.usage(this, System.out);
        return 0;
    }

    public static void main(String[] args) {
        CommandLine cmd = new CommandLine(new RepoDnaCli());
        
        // Structured error handler to suppress stack traces for expected domain exceptions
        cmd.setExecutionExceptionHandler((ex, commandLine, parseResult) -> {
            if (ex instanceof RepoDnaException) {
                System.err.println("Error: " + ex.getMessage());
                return 1;
            }
            ex.printStackTrace(); // dump full traces for unexpected system errors
            return 1;
        });
        
        int exitCode = cmd.execute(args);
        System.exit(exitCode);
    }
}
