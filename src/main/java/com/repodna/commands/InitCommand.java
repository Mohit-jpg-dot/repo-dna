package com.repodna.commands;

import com.repodna.config.AppConfig;
import com.repodna.config.ConfigManager;
import com.repodna.exception.CommandException;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

/**
 * Command to initialize the RepoDNA configuration layout in a target directory.
 */
@Command(
    name = "init",
    description = "Initialize RepoDNA inside a repository",
    mixinStandardHelpOptions = true
)
public class InitCommand implements Callable<Integer> {

    @Option(names = {"-d", "--dir"}, description = "Target directory to initialize (default: current directory)")
    private Path dir = Path.of(".");

    @Override
    public Integer call() {
        try {
            Path targetDir = dir.toAbsolutePath().normalize();
            if (!Files.exists(targetDir)) {
                Files.createDirectories(targetDir);
            }

            ConfigManager configManager = new ConfigManager(targetDir);
            Path repoDnaDir = configManager.getConfigDir();

            // Create configuration and tracking subdirectories
            Files.createDirectories(repoDnaDir);
            Files.createDirectories(repoDnaDir.resolve("cache"));
            Files.createDirectories(repoDnaDir.resolve("reports"));
            Files.createDirectories(repoDnaDir.resolve("future"));

            // Initialize default project layout configuration
            AppConfig config = new AppConfig(
                targetDir.toString(),
                repoDnaDir.resolve("cache").toString(),
                repoDnaDir.resolve("reports").toString()
            );
            configManager.save(config);

            System.out.println("Repository initialized successfully.");
            return 0;
        } catch (Exception e) {
            throw new CommandException("Failed to initialize repository: " + e.getMessage(), e);
        }
    }
}
