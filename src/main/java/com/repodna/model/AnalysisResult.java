package com.repodna.model;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

public class AnalysisResult {
    private ProjectInfo projectInfo;
    private List<SourceFile> sourceFiles;
    private List<Object> parsedFiles;
    private Object dependencyGraph;
    private List<Object> discoveredPatterns;
    private Object dnaProfile;
    private Object healthReport;
    private List<Object> engineeringRules;
    private Instant startedAt;
    private Instant completedAt;

    public ProjectInfo getProjectInfo() { return projectInfo; }
    public void setProjectInfo(ProjectInfo projectInfo) { this.projectInfo = projectInfo; }

    public List<SourceFile> getSourceFiles() { return sourceFiles; }
    public void setSourceFiles(List<SourceFile> sourceFiles) { this.sourceFiles = sourceFiles; }

    public List<Object> getParsedFiles() { return parsedFiles; }
    public void setParsedFiles(List<Object> parsedFiles) { this.parsedFiles = parsedFiles; }

    public Object getDependencyGraph() { return dependencyGraph; }
    public void setDependencyGraph(Object dependencyGraph) { this.dependencyGraph = dependencyGraph; }

    public List<Object> getDiscoveredPatterns() { return discoveredPatterns; }
    public void setDiscoveredPatterns(List<Object> discoveredPatterns) { this.discoveredPatterns = discoveredPatterns; }

    public Object getDnaProfile() { return dnaProfile; }
    public void setDnaProfile(Object dnaProfile) { this.dnaProfile = dnaProfile; }

    public Object getHealthReport() { return healthReport; }
    public void setHealthReport(Object healthReport) { this.healthReport = healthReport; }

    public List<Object> getEngineeringRules() { return engineeringRules; }
    public void setEngineeringRules(List<Object> engineeringRules) { this.engineeringRules = engineeringRules; }

    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }

    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }

    public Duration duration() {
        if (startedAt != null && completedAt != null) {
            return Duration.between(startedAt, completedAt);
        }
        return Duration.ZERO;
    }

    public long sourceFileCount() {
        if (sourceFiles == null) return 0;
        return sourceFiles.stream().filter(f -> f.type() == SourceFile.FileType.SOURCE).count();
    }

    public long javaFileCount() {
        if (sourceFiles == null) return 0;
        return sourceFiles.stream().filter(f -> "java".equalsIgnoreCase(f.language())).count();
    }
}
