package com.repodna.commands;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import java.nio.file.Path;
import java.util.concurrent.Callable;

/**
 * Stub command for the future Explanation engine.
 */
@Command(
    name = "explain",
    description = "Explain rules and architectural patterns with evidence",
    mixinStandardHelpOptions = true
)
public class ExplainCommand implements Callable<Integer> {

    @Parameters(index = "0", arity = "0..1", description = "The target directory to check", defaultValue = ".")
    private Path dir = Path.of(".");

    @Override
    public Integer call() {
        System.out.println("Repository explanation is not available yet.");
        System.out.println("Future module.");
        return 0;
    }
}
