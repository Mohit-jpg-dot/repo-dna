package com.repodna.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.repodna.exception.ConfigurationException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Manages saving and loading RepoDNA JSON configurations.
 */
public class ConfigManager {
    private final Path configDir;
    private final Path configFile;
    private final ObjectMapper mapper;

    public ConfigManager(Path repoRoot) {
        this.configDir = repoRoot.resolve(".repo-dna");
        this.configFile = configDir.resolve("config.json");
        this.mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    }

    /**
     * Checks if config file exists.
     */
    public boolean exists() {
        return Files.exists(configFile);
    }

    /**
     * Loads AppConfig from the config file.
     */
    public AppConfig load() {
        if (!exists()) {
            throw new ConfigurationException("Configuration file does not exist at " + configFile);
        }
        try {
            return mapper.readValue(configFile.toFile(), AppConfig.class);
        } catch (IOException e) {
            throw new ConfigurationException("Failed to read configuration: " + e.getMessage(), e);
        }
    }

    /**
     * Saves AppConfig to the config file. Creates directory if missing.
     */
    public void save(AppConfig config) {
        try {
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
            }
            mapper.writeValue(configFile.toFile(), config);
        } catch (IOException e) {
            throw new ConfigurationException("Failed to write configuration: " + e.getMessage(), e);
        }
    }

    public Path getConfigFile() {
        return configFile;
    }

    public Path getConfigDir() {
        return configDir;
    }
}
