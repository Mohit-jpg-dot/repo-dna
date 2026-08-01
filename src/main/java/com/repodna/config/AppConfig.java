package com.repodna.config;

import java.util.HashMap;
import java.util.Map;

/**
 * Data model for RepoDNA project settings stored in JSON.
 */
public class AppConfig {
    private String projectPath;
    private String cachePath;
    private String outputPath;
    private String version = "1.0.0";
    private Map<String, Object> futureOptions = new HashMap<>();

    public AppConfig() {}

    public AppConfig(String projectPath, String cachePath, String outputPath) {
        this.projectPath = projectPath;
        this.cachePath = cachePath;
        this.outputPath = outputPath;
    }

    public String getProjectPath() {
        return projectPath;
    }

    public void setProjectPath(String projectPath) {
        this.projectPath = projectPath;
    }

    public String getCachePath() {
        return cachePath;
    }

    public void setCachePath(String cachePath) {
        this.cachePath = cachePath;
    }

    public String getOutputPath() {
        return outputPath;
    }

    public void setOutputPath(String outputPath) {
        this.outputPath = outputPath;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Map<String, Object> getFutureOptions() {
        return futureOptions;
    }

    public void setFutureOptions(Map<String, Object> futureOptions) {
        this.futureOptions = futureOptions;
    }
}
