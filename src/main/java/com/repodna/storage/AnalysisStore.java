package com.repodna.storage;

import com.repodna.model.ProjectInfo;
import com.repodna.model.SourceFile;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class AnalysisStore {
    private final DatabaseManager dbManager;

    public AnalysisStore(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public long saveAnalysisRun(ProjectInfo info) {
        String sql = "INSERT INTO analysis_runs (project_name, project_path, language, framework, build_tool, started_at) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
             
            pstmt.setString(1, info.name());
            pstmt.setString(2, info.rootPath().toString());
            pstmt.setString(3, info.language());
            pstmt.setString(4, info.framework());
            pstmt.setString(5, info.buildTool());
            pstmt.setString(6, info.analyzedAt() != null ? info.analyzedAt().toString() : Instant.now().toString());
            
            pstmt.executeUpdate();
            
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
            throw new SQLException("Creating analysis run failed, no ID obtained.");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save analysis run", e);
        }
    }

    public Optional<Long> getLastAnalysisRun() {
        String sql = "SELECT id FROM analysis_runs ORDER BY started_at DESC LIMIT 1";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
             
            if (rs.next()) {
                return Optional.of(rs.getLong("id"));
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get last analysis run", e);
        }
    }

    public void saveSourceFiles(List<SourceFile> files, long runId) {
        String sql = "INSERT INTO source_files (run_id, path, file_type, language, size_bytes, last_modified) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
             
            conn.setAutoCommit(false);
            
            for (SourceFile file : files) {
                pstmt.setLong(1, runId);
                pstmt.setString(2, file.path().toString());
                pstmt.setString(3, file.type().name());
                pstmt.setString(4, file.language());
                pstmt.setLong(5, file.sizeBytes());
                pstmt.setString(6, file.lastModified() != null ? file.lastModified().toString() : null);
                pstmt.addBatch();
            }
            
            pstmt.executeBatch();
            conn.commit();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save source files batch", e);
        }
    }

    public List<SourceFile> getSourceFiles(long runId) {
        String sql = "SELECT path, file_type, language, size_bytes, last_modified FROM source_files WHERE run_id = ?";
        List<SourceFile> files = new ArrayList<>();
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
             
            pstmt.setLong(1, runId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String lastModifiedStr = rs.getString("last_modified");
                    files.add(new SourceFile(
                        Path.of(rs.getString("path")),
                        null,
                        SourceFile.FileType.valueOf(rs.getString("file_type")),
                        rs.getString("language"),
                        rs.getLong("size_bytes"),
                        lastModifiedStr != null ? Instant.parse(lastModifiedStr) : null
                    ));
                }
            }
            return files;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to retrieve source files", e);
        }
    }

    public void saveHealthScores(Map<String, Integer> scores, long runId) {
        String sql = "INSERT INTO health_scores (run_id, dimension, score, grade) VALUES (?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
             
            conn.setAutoCommit(false);
            
            for (Map.Entry<String, Integer> entry : scores.entrySet()) {
                pstmt.setLong(1, runId);
                pstmt.setString(2, entry.getKey());
                int score = entry.getValue();
                pstmt.setInt(3, score);
                pstmt.setString(4, getGrade(score));
                pstmt.addBatch();
            }
            
            pstmt.executeBatch();
            conn.commit();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save health scores", e);
        }
    }

    public Map<String, Integer> getHealthScores(long runId) {
        String sql = "SELECT dimension, score FROM health_scores WHERE run_id = ?";
        Map<String, Integer> scores = new HashMap<>();
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
             
            pstmt.setLong(1, runId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    scores.put(rs.getString("dimension"), rs.getInt("score"));
                }
            }
            return scores;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to retrieve health scores", e);
        }
    }

    public void savePatterns(List<com.repodna.discovery.model.DiscoveredPattern> patterns, long runId) {
        String sql = "INSERT INTO discovered_patterns (run_id, pattern_id, category, description, confidence, occurrences, total_opportunities, reasoning, evidence_json) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            conn.setAutoCommit(false);
            for (com.repodna.discovery.model.DiscoveredPattern p : patterns) {
                pstmt.setLong(1, runId);
                pstmt.setString(2, p.id());
                pstmt.setString(3, p.category());
                pstmt.setString(4, p.description());
                pstmt.setDouble(5, p.confidence());
                pstmt.setInt(6, p.occurrences());
                pstmt.setInt(7, p.totalOpportunities());
                pstmt.setString(8, p.reasoning());
                pstmt.setString(9, mapper.writeValueAsString(p.evidence()));
                pstmt.addBatch();
            }
            pstmt.executeBatch();
            conn.commit();
        } catch (Exception e) {
            throw new RuntimeException("Failed to save patterns", e);
        }
    }

    public void saveRules(List<com.repodna.rules.model.EngineeringRule> rules, long runId) {
        String sql = "INSERT INTO engineering_rules (run_id, rule_id, category, description, rationale, confidence, supporting_examples, evidence_json, violations_json) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            conn.setAutoCommit(false);
            for (com.repodna.rules.model.EngineeringRule r : rules) {
                pstmt.setLong(1, runId);
                pstmt.setString(2, r.id());
                pstmt.setString(3, r.category());
                pstmt.setString(4, r.description());
                pstmt.setString(5, r.rationale());
                pstmt.setDouble(6, r.confidence());
                pstmt.setInt(7, r.supportingExamples());
                pstmt.setString(8, mapper.writeValueAsString(r.evidence()));
                pstmt.setString(9, mapper.writeValueAsString(r.violations()));
                pstmt.addBatch();
            }
            pstmt.executeBatch();
            conn.commit();
        } catch (Exception e) {
            throw new RuntimeException("Failed to save rules", e);
        }
    }

    public List<com.repodna.rules.model.EngineeringRule> getRules(long runId) {
        String sql = "SELECT rule_id, category, description, rationale, confidence, supporting_examples, evidence_json, violations_json FROM engineering_rules WHERE run_id = ?";
        List<com.repodna.rules.model.EngineeringRule> rules = new ArrayList<>();
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, runId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String evJson = rs.getString("evidence_json");
                    String viJson = rs.getString("violations_json");
                    
                    List<com.repodna.discovery.model.PatternEvidence> evidence = null;
                    if (evJson != null) {
                        try {
                            evidence = mapper.readValue(evJson, 
                                mapper.getTypeFactory().constructCollectionType(List.class, com.repodna.discovery.model.PatternEvidence.class));
                        } catch (Exception ex) {
                            evidence = new ArrayList<>();
                        }
                    }
                    
                    List<com.repodna.rules.model.RuleViolation> violations = null;
                    if (viJson != null) {
                        try {
                            violations = mapper.readValue(viJson, 
                                mapper.getTypeFactory().constructCollectionType(List.class, com.repodna.rules.model.RuleViolation.class));
                        } catch (Exception ex) {
                            violations = new ArrayList<>();
                        }
                    }
                    
                    rules.add(new com.repodna.rules.model.EngineeringRule(
                        rs.getString("rule_id"),
                        rs.getString("category"),
                        rs.getString("description"),
                        rs.getString("rationale"),
                        rs.getDouble("confidence"),
                        rs.getInt("supporting_examples"),
                        evidence != null ? evidence : new ArrayList<>(),
                        violations != null ? violations : new ArrayList<>()
                    ));
                }
            }
            return rules;
        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve rules", e);
        }
    }

    public List<com.repodna.discovery.model.DiscoveredPattern> getPatterns(long runId) {
        String sql = "SELECT pattern_id, category, description, confidence, occurrences, total_opportunities, reasoning, evidence_json FROM discovered_patterns WHERE run_id = ?";
        List<com.repodna.discovery.model.DiscoveredPattern> patterns = new ArrayList<>();
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, runId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String evJson = rs.getString("evidence_json");
                    List<com.repodna.discovery.model.PatternEvidence> evidence = null;
                    if (evJson != null) {
                        try {
                            evidence = mapper.readValue(evJson, 
                                mapper.getTypeFactory().constructCollectionType(List.class, com.repodna.discovery.model.PatternEvidence.class));
                        } catch (Exception ex) {
                            evidence = new ArrayList<>();
                        }
                    }
                    patterns.add(new com.repodna.discovery.model.DiscoveredPattern(
                        rs.getString("pattern_id"),
                        rs.getString("category"),
                        rs.getString("description"),
                        rs.getDouble("confidence"),
                        rs.getInt("occurrences"),
                        rs.getInt("total_opportunities"),
                        evidence != null ? evidence : new ArrayList<>(),
                        rs.getString("reasoning")
                    ));
                }
            }
            return patterns;
        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve patterns", e);
        }
    }

    private String getGrade(int score) {
        if (score >= 90) return "A";
        if (score >= 85) return "B+";
        if (score >= 80) return "B";
        if (score >= 75) return "C+";
        if (score >= 70) return "C";
        if (score >= 60) return "D";
        return "F";
    }
}
