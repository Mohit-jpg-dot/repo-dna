package com.repodna.discovery.detectors;

import com.repodna.discovery.AnalysisContext;
import com.repodna.discovery.PatternDetector;
import com.repodna.discovery.model.DiscoveredPattern;
import com.repodna.discovery.model.PatternEvidence;
import com.repodna.parser.model.ClassDecl;
import com.repodna.parser.model.FieldDecl;

import java.util.ArrayList;
import java.util.List;

public class LoggingPatternDetector implements PatternDetector {
    @Override
    public String name() {
        return "Logging Pattern Detector";
    }

    @Override
    public List<DiscoveredPattern> detect(AnalysisContext context) {
        List<DiscoveredPattern> patterns = new ArrayList<>();
        List<ClassDecl> classes = context.allClasses();
        
        int loggerOccurrences = 0;
        int slf4jAnnotations = 0;
        List<PatternEvidence> evidence = new ArrayList<>();
        
        for (ClassDecl cls : classes) {
            boolean hasLogger = false;
            for (FieldDecl field : cls.fields()) {
                if (field.type() != null && (field.type().equals("Logger") || field.type().endsWith(".Logger"))) {
                    hasLogger = true;
                    loggerOccurrences++;
                    evidence.add(new PatternEvidence(null, 0, cls.name() + "." + field.name(), "Logger field declaration"));
                }
            }
            if (AnalysisContext.hasAnnotation(cls, "Slf4j")) {
                slf4jAnnotations++;
                evidence.add(new PatternEvidence(null, 0, cls.name(), "@Slf4j lombok annotation"));
            }
        }
        
        if (loggerOccurrences > 0) {
            patterns.add(DiscoveredPattern.of(
                "logging.standard", "logging", "Standard Logger Usage",
                loggerOccurrences, loggerOccurrences + slf4jAnnotations, evidence,
                "Classes explicitly define Logger instances."
            ));
        }
        
        if (slf4jAnnotations > 0) {
            patterns.add(DiscoveredPattern.of(
                "logging.lombok-slf4j", "logging", "Lombok @Slf4j Usage",
                slf4jAnnotations, loggerOccurrences + slf4jAnnotations, evidence,
                "Classes use Lombok @Slf4j for logging."
            ));
        }
        
        return patterns;
    }
}
