package com.repodna.dna;

import com.repodna.discovery.model.Pattern;
import com.repodna.discovery.model.PatternSummary;
import com.repodna.graph.RepositoryKnowledgeGraph;
import com.repodna.parser.model.ProjectModel;
import com.repodna.scanner.model.ScannerResult;

import java.time.Instant;
import java.util.List;

/**
 * Immutable representation of a repository's complete engineering DNA profile.
 * Produced by {@link DnaEngine} after running the full analysis pipeline.
 */
public record DnaProfile(
    ScannerResult scannerResult,
    ProjectModel projectModel,
    RepositoryKnowledgeGraph graph,
    List<Pattern> patterns,
    PatternSummary summary,
    Instant timestamp
) {}
