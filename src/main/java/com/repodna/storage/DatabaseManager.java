package com.repodna.storage;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager implements AutoCloseable {
    @Override
    public void close() {}
    private final Path dbPath;
    private final String jdbcUrl;

    public DatabaseManager(Path dbPath) {
        this.dbPath = dbPath;
        this.jdbcUrl = "jdbc:sqlite:" + dbPath.toAbsolutePath().toString();
    }

    public void initialize() {
        try {
            if (dbPath.getParent() != null && !Files.exists(dbPath.getParent())) {
                Files.createDirectories(dbPath.getParent());
            }

            try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
                String schemaSql = loadSchemaSql();
                String[] statements = schemaSql.split(";");
                for (String sqlStmt : statements) {
                    if (!sqlStmt.trim().isEmpty()) {
                        stmt.execute(sqlStmt);
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize database at " + dbPath, e);
        }
    }

    public Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(jdbcUrl);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA journal_mode = WAL;");
            stmt.execute("PRAGMA synchronous = NORMAL;");
        }
        return conn;
    }

    public boolean isInitialized() {
        if (!Files.exists(dbPath)) return false;
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='schema_version'")) {
            return rs.next();
        } catch (SQLException e) {
            return false;
        }
    }

    private String loadSchemaSql() throws Exception {
        try (InputStream is = getClass().getResourceAsStream("/schema.sql")) {
            if (is == null) {
                throw new IllegalStateException("schema.sql not found in resources");
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
