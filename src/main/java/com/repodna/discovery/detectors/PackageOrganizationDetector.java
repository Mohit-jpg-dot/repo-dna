package com.repodna.discovery.detectors;

import com.repodna.discovery.AnalysisContext;
import com.repodna.discovery.PatternDetector;
import com.repodna.discovery.model.DiscoveredPattern;
import com.repodna.discovery.model.PatternEvidence;
import com.repodna.parser.model.ParsedFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PackageOrganizationDetector implements PatternDetector {
    @Override
    public String name() {
        return "Package Organization Detector";
    }

    @Override
    public List<DiscoveredPattern> detect(AnalysisContext context) {
        List<DiscoveredPattern> patterns = new ArrayList<>();
        List<ParsedFile> files = context.sourceOnlyFiles();
        
        if (files.isEmpty()) return patterns;
        
        Map<String, Long> packageCounts = files.stream()
                .filter(f -> f.packageName() != null && !f.packageName().isEmpty())
                .collect(Collectors.groupingBy(ParsedFile::packageName, Collectors.counting()));
                
        int totalPackages = packageCounts.size();
        List<PatternEvidence> evidence = new ArrayList<>();
        
        boolean hasLayerPackages = packageCounts.keySet().stream()
                .anyMatch(p -> p.endsWith(".controller") || p.endsWith(".service") || p.endsWith(".repository"));
                
        if (hasLayerPackages) {
            evidence.add(new PatternEvidence(null, 0, "Package structure", "Found package-by-layer organization"));
            patterns.add(DiscoveredPattern.of(
                "package.by-layer", "organization", "Package By Layer",
                1, 1, evidence,
                "The project appears to organize packages by technical layers."
            ));
        }

        return patterns;
    }
}
