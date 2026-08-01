package com.repodna.discovery.detectors;

import com.repodna.discovery.AnalysisContext;
import com.repodna.discovery.PatternDetector;
import com.repodna.discovery.model.DiscoveredPattern;
import com.repodna.discovery.model.PatternEvidence;
import com.repodna.parser.model.ClassDecl;

import java.util.ArrayList;
import java.util.List;

public class ExceptionHandlingDetector implements PatternDetector {
    @Override
    public String name() {
        return "Exception Handling Detector";
    }

    @Override
    public List<DiscoveredPattern> detect(AnalysisContext context) {
        List<DiscoveredPattern> patterns = new ArrayList<>();
        List<ClassDecl> classes = context.allClasses();
        
        int exceptionClasses = 0;
        int totalOpportunities = 0;
        List<PatternEvidence> evidence = new ArrayList<>();
        
        for (ClassDecl cls : classes) {
            if (cls.superClass() != null && (cls.superClass().equals("Exception") || cls.superClass().equals("RuntimeException"))) {
                exceptionClasses++;
                evidence.add(new PatternEvidence(null, 0, cls.name(), "Custom exception class"));
            }
            
            if (AnalysisContext.hasAnnotation(cls, "ControllerAdvice") || AnalysisContext.hasAnnotation(cls, "RestControllerAdvice")) {
                totalOpportunities++;
                evidence.add(new PatternEvidence(null, 0, cls.name(), "Global exception handler"));
            }
        }
        
        if (exceptionClasses > 0) {
            patterns.add(DiscoveredPattern.of(
                "exception.custom", "exception-handling", "Custom Exception Usage",
                exceptionClasses, exceptionClasses, evidence,
                "Project uses custom exception classes for domain-specific errors."
            ));
        }
        
        if (totalOpportunities > 0) {
             patterns.add(DiscoveredPattern.of(
                "exception.global-handler", "exception-handling", "Global Exception Handler (@ControllerAdvice)",
                totalOpportunities, totalOpportunities, evidence,
                "Project uses a global exception handler for centralized error management."
            ));
        }
        
        return patterns;
    }
}
