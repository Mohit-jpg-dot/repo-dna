package com.repodna.rules.model;

import com.repodna.discovery.model.PatternEvidence;
import java.util.List;

public record EngineeringRule(
    String id,
    String category,
    String description,
    String rationale,
    double confidence,
    int supportingExamples,
    List<PatternEvidence> evidence,
    List<RuleViolation> violations
) {}
