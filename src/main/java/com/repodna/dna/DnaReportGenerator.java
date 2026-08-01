package com.repodna.dna;

import com.repodna.discovery.model.*;
import com.repodna.graph.model.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Generates markdown reports (REPO_DNA.md, AGENTS.md, ARCHITECTURE.md, PROJECT_RULES.md)
 * based on the analysed repository DNA Profile.
 */
public class DnaReportGenerator {

    /**
     * Generates and writes all report files to the target repository directory.
     */
    public static void generateReports(DnaProfile profile, Path targetDir) throws IOException {
        writeRepoDna(profile, targetDir.resolve("REPO_DNA.md"));
        writeAgents(profile, targetDir.resolve("AGENTS.md"));
        writeArchitecture(profile, targetDir.resolve("ARCHITECTURE.md"));
        writeProjectRules(profile, targetDir.resolve("PROJECT_RULES.md"));
    }

    private static void writeRepoDna(DnaProfile profile, Path filePath) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("# Repository Engineering DNA Profile (REPO_DNA.md)\n\n");
        sb.append("This document outlines the engineering patterns discovered in the repository.\n\n");
        
        sb.append("## Summary\n");
        sb.append("- **Overall Confidence**: ").append(profile.summary().overallConfidence() * 100).append("%\n");
        sb.append("- **Total Discovered Patterns**: ").append(profile.summary().totalPatterns()).append("\n\n");

        sb.append("## Discovered Patterns\n\n");
        for (Pattern p : profile.patterns()) {
            sb.append("### ").append(p.name()).append(" (ID: `").append(p.id()).append("`)\n");
            sb.append("- **Category**: ").append(p.category()).append("\n");
            sb.append("- **Description**: ").append(p.description()).append("\n");
            sb.append("- **Confidence**: ").append(p.confidence().score() * 100).append("% (Support: ").append(p.confidence().supportCount())
              .append(", Violations: ").append(p.confidence().violationCount()).append(")\n");
            sb.append("- **Stability**: ").append(p.history().stability()).append(" (Age: ").append(p.history().ageCommits()).append(" commits)\n\n");

            if (!p.evidenceList().isEmpty()) {
                sb.append("#### Evidence\n");
                for (PatternEvidence ev : p.evidenceList()) {
                    sb.append("- `").append(ev.nodeId()).append("`: ").append(ev.description()).append("\n");
                }
                sb.append("\n");
            }

            if (!p.violationsList().isEmpty()) {
                sb.append("#### Violations / Outliers\n");
                for (PatternViolation viol : p.violationsList()) {
                    sb.append("- `").append(viol.nodeId()).append("`: ").append(viol.description()).append("\n");
                }
                sb.append("\n");
            }
            sb.append("---\n\n");
        }

        Files.writeString(filePath, sb.toString());
    }

    private static void writeAgents(DnaProfile profile, Path filePath) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("# AI Agent Coding Instructions (AGENTS.md)\n\n");
        sb.append("> **Important**: This repository follows strict engineering rules. Adhere to these patterns when generating code.\n\n");

        sb.append("## Project Context\n");
        sb.append("- **Project Name**: ").append(profile.scannerResult().repoName()).append("\n");
        sb.append("- **Primary Language**: ").append(profile.scannerResult().primaryLanguage().name()).append("\n");
        sb.append("- **Build System**: ").append(profile.scannerResult().buildSystem().name()).append("\n\n");

        sb.append("## Core Signatures learned from Repository\n");
        List<Pattern> dominant = profile.patterns().stream()
            .filter(p -> p.confidence().score() >= 0.7)
            .collect(Collectors.toList());

        for (Pattern p : dominant) {
            sb.append("- **").append(p.name()).append("**: ").append(p.description())
              .append(" (").append(p.confidence().score() * 100).append("% consistency)\n");
        }
        sb.append("\n");

        sb.append("## DOs and DONTs\n");
        sb.append("### DO\n");
        
        // Add DOs from dominant patterns
        for (Pattern p : dominant) {
            if ("naming-suffix-service".equals(p.id())) {
                sb.append("- Name all classes annotated with `@Service` with the suffix `Service`.\n");
            } else if ("naming-suffix-controller".equals(p.id())) {
                sb.append("- Name all REST API classes annotated with `@RestController` with the suffix `Controller`.\n");
            } else if ("naming-suffix-repository".equals(p.id())) {
                sb.append("- Name all repository database access interfaces with the suffix `Repository`.\n");
            } else if ("spring-constructor-injection".equals(p.id())) {
                sb.append("- Use constructor-based dependency injection in all Spring-managed components.\n");
            } else if ("test-naming-test".equals(p.id())) {
                sb.append("- End unit test class names with the `Test` suffix.\n");
            }
        }
        sb.append("- Write clean Javadoc documentation on all public API methods.\n");
        sb.append("- Place database entities in `.entity` or `.model` packages.\n\n");

        sb.append("### DONT\n");
        
        // Add DONTs based on violations of dominant patterns
        for (Pattern p : dominant) {
            if ("spring-field-injection".equals(p.id()) && p.confidence().score() < 0.3) {
                sb.append("- Do not use Field Injection (`@Autowired` on fields) inside Spring components; always prefer Constructor Injection.\n");
            } else if ("spring-constructor-injection".equals(p.id()) && p.confidence().score() >= 0.7) {
                sb.append("- Do not use `@Autowired` field injection inside classes; prefer package-private final constructor fields.\n");
            } else if ("naming-exception".equals(p.id())) {
                sb.append("- Do not create exception classes that do not end with the `Exception` suffix.\n");
            } else if ("naming-dto".equals(p.id())) {
                sb.append("- Do not skip naming suffixes like `DTO`, `Dto`, `Request`, or `Response` for files inside transfer packages.\n");
            }
        }
        sb.append("- Do not introduce circular package dependencies.\n");
        sb.append("- Do not write JUnit tests ending with suffixes other than standard project test conventions.\n");

        Files.writeString(filePath, sb.toString());
    }

    private static void writeArchitecture(DnaProfile profile, Path filePath) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("# Project Architecture Overview (ARCHITECTURE.md)\n\n");
        
        // Calculate a mock integrity score based on violations and package coupling
        int anomalies = 0;
        for (Pattern p : profile.patterns()) {
            anomalies += p.confidence().violationCount();
        }

        int integrityScore = Math.max(0, 100 - (anomalies * 5));
        String grade = "A";
        if (integrityScore < 60) grade = "F";
        else if (integrityScore < 70) grade = "D";
        else if (integrityScore < 80) grade = "C";
        else if (integrityScore < 90) grade = "B";

        sb.append("## Architectural Integrity\n");
        sb.append("- **Overall Architecture Grade**: ").append(grade).append(" (Score: ").append(integrityScore).append("/100)\n");
        sb.append("- **Architecture Drift**: ").append(anomalies > 0 ? (anomalies * 2) + "%" : "0.0%").append("\n\n");

        sb.append("## Health Dimension Scores\n");
        
        // Filter some patterns to show dimensional scores
        for (Pattern p : profile.patterns()) {
            if ("package-coupling-boundaries".equals(p.id())) {
                sb.append("- **Architecture Boundary**: ").append(p.confidence().score() * 100).append("/100 (")
                  .append(p.confidence().score() >= 0.9 ? "A" : "B").append(")\n");
            } else if ("circular-dependencies".equals(p.id())) {
                sb.append("- **Dependency Integrity**: ").append(p.confidence().score() * 100).append("/100\n");
            }
        }
        sb.append("\n");

        sb.append("## Mermaid Component/Layer Diagram\n\n");
        sb.append("```mermaid\n");
        sb.append("graph TD\n");
        
        // Extract package dependencies to render a basic package graph
        List<RkgEdge> pkgEdges = profile.graph().getAllEdges().stream()
            .filter(e -> e.type() == RkgEdgeType.DEPENDS_ON)
            .collect(Collectors.toList());

        List<String> renderedEdges = new ArrayList<>();
        for (RkgEdge edge : pkgEdges) {
            RkgNode src = profile.graph().getNodeById(edge.source());
            RkgNode tgt = profile.graph().getNodeById(edge.target());
            if (src != null && tgt != null) {
                String srcPkg = (String) src.metadata().get("package");
                String tgtPkg = (String) tgt.metadata().get("package");
                if (srcPkg != null && tgtPkg != null && !srcPkg.equals(tgtPkg)) {
                    String cleanSrc = srcPkg.substring(srcPkg.lastIndexOf('.') + 1);
                    String cleanTgt = tgtPkg.substring(tgtPkg.lastIndexOf('.') + 1);
                    String edgeStr = "    " + cleanSrc + " --> " + cleanTgt;
                    if (!renderedEdges.contains(edgeStr)) {
                        renderedEdges.add(edgeStr);
                    }
                }
            }
        }

        if (renderedEdges.isEmpty()) {
            sb.append("    Controller[Controller Layer] --> Service[Service Layer]\n");
            sb.append("    Service --> Repository[Repository Layer]\n");
        } else {
            for (String edge : renderedEdges) {
                sb.append(edge).append("\n");
            }
        }
        sb.append("```\n");

        Files.writeString(filePath, sb.toString());
    }

    private static void writeProjectRules(DnaProfile profile, Path filePath) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("# Project Engineering Rules (PROJECT_RULES.md)\n\n");
        sb.append("This file lists the enforceable engineering rules learned from this repository.\n\n");

        int index = 1;
        for (Pattern p : profile.patterns()) {
            if (p.confidence().score() >= 0.7) {
                sb.append("## Rule ").append(index++).append(": ").append(p.name()).append("\n");
                sb.append("- **Enforcement Category**: ").append(p.category()).append("\n");
                sb.append("- **Rule Statement**: ").append(p.description()).append("\n");
                sb.append("- **Confidence Threshold**: ").append(p.confidence().score() * 100).append("%\n");
                sb.append("- **Violations Allowed**: No\n\n");
            }
        }

        Files.writeString(filePath, sb.toString());
    }
}
