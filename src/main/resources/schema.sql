CREATE TABLE IF NOT EXISTS schema_version (
    version INTEGER PRIMARY KEY,
    applied_at TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS analysis_runs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    project_name TEXT NOT NULL,
    project_path TEXT NOT NULL,
    language TEXT NOT NULL,
    framework TEXT,
    build_tool TEXT,
    started_at TEXT NOT NULL,
    completed_at TEXT,
    source_file_count INTEGER DEFAULT 0,
    java_file_count INTEGER DEFAULT 0,
    test_file_count INTEGER DEFAULT 0
);

CREATE TABLE IF NOT EXISTS source_files (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    run_id INTEGER NOT NULL REFERENCES analysis_runs(id),
    path TEXT NOT NULL,
    file_type TEXT NOT NULL,
    language TEXT,
    size_bytes INTEGER,
    last_modified TEXT
);

CREATE TABLE IF NOT EXISTS discovered_patterns (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    run_id INTEGER NOT NULL REFERENCES analysis_runs(id),
    pattern_id TEXT NOT NULL,
    category TEXT NOT NULL,
    description TEXT NOT NULL,
    confidence REAL NOT NULL,
    occurrences INTEGER,
    total_opportunities INTEGER,
    reasoning TEXT,
    evidence_json TEXT
);

CREATE TABLE IF NOT EXISTS engineering_rules (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    run_id INTEGER NOT NULL REFERENCES analysis_runs(id),
    rule_id TEXT NOT NULL,
    category TEXT NOT NULL,
    description TEXT NOT NULL,
    rationale TEXT,
    confidence REAL NOT NULL,
    supporting_examples INTEGER,
    evidence_json TEXT,
    violations_json TEXT
);

CREATE TABLE IF NOT EXISTS health_scores (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    run_id INTEGER NOT NULL REFERENCES analysis_runs(id),
    dimension TEXT NOT NULL,
    score INTEGER NOT NULL,
    grade TEXT NOT NULL,
    strengths_json TEXT,
    improvements_json TEXT
);

CREATE INDEX IF NOT EXISTS idx_source_files_run ON source_files(run_id);
CREATE INDEX IF NOT EXISTS idx_patterns_run ON discovered_patterns(run_id);
CREATE INDEX IF NOT EXISTS idx_rules_run ON engineering_rules(run_id);
CREATE INDEX IF NOT EXISTS idx_health_run ON health_scores(run_id);

INSERT OR IGNORE INTO schema_version (version) VALUES (1);
