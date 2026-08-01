package com.repodna.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.repodna.parser.model.ParsedFile;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Optional;

/**
 * Manages incremental AST parsing caches stored in SQLite database.
 */
public class AstCacheStore {
    private final DatabaseManager dbManager;
    private final ObjectMapper mapper;

    public AstCacheStore(DatabaseManager dbManager) {
        this.dbManager = dbManager;
        this.mapper = new ObjectMapper();
    }

    /**
     * Retrieves parsed AST from cache if file path and SHA-256 hash match.
     */
    public Optional<ParsedFile> get(String path, String sha256) {
        String sql = "SELECT parsed_data_json FROM parsed_files_cache WHERE path = ? AND sha256 = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, path);
            pstmt.setString(2, sha256);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String json = rs.getString("parsed_data_json");
                    ParsedFile parsedFile = mapper.readValue(json, ParsedFile.class);
                    return Optional.of(parsedFile);
                }
            }
            return Optional.empty();
        } catch (Exception e) {
            // Ignore cache read failures and fallback to re-parsing
            return Optional.empty();
        }
    }

    /**
     * Saves parsed AST to cache, associating it with the file path and SHA-256 hash.
     */
    public void put(String path, String sha256, ParsedFile data) {
        String sql = "INSERT OR REPLACE INTO parsed_files_cache (path, sha256, parsed_data_json, updated_at) VALUES (?, ?, ?, datetime('now'))";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, path);
            pstmt.setString(2, sha256);
            pstmt.setString(3, mapper.writeValueAsString(data));
            pstmt.executeUpdate();
        } catch (Exception e) {
            // Ignore cache write failures to ensure pipeline proceeds
        }
    }
}
