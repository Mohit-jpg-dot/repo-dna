package com.repodna.commands;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import java.nio.file.Path;
import java.util.concurrent.Callable;

/**
 * Stub command for the future Health engine.
 */
@Command(
    name = "health",
    description = "Display repository health analysis metrics",
    mixinStandardHelpOptions = true
)
public class HealthCommand implements Callable<Integer> {

    @Parameters(index = "0", arity = "0..1", description = "The target directory to check", defaultValue = ".")
    private Path dir = Path.of(".");

    @Override
    public Integer call() {
        System.out.println("Repository health analysis is not available yet.");
        System.out.println("Future module: Repository Health Engine.");
        return 0;
    }
}
