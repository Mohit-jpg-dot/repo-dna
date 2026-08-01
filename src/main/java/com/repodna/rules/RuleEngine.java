package com.repodna.rules;

import com.repodna.discovery.AnalysisContext;
import com.repodna.rules.model.EngineeringRule;
import com.repodna.rules.model.RuleViolation;
import com.repodna.graph.LayerDetector;
import com.repodna.parser.model.ClassDecl;
import com.repodna.parser.model.MethodDecl;
import com.repodna.parser.model.ParsedFile;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class RuleEngine {
    /**
     * Analyzes the context and checks for violations of the generated rules.
     * Populates the violations field in each rule.
     */
    public List<EngineeringRule> evaluateRules(AnalysisContext context, List<EngineeringRule> rules) {
        List<EngineeringRule> evaluatedRules = new ArrayList<>();
        
        for (EngineeringRule rule : rules) {
            List<RuleViolation> violations = new ArrayList<>();
            
            // Check for specific rule violations
            if ("naming.service-suffix".equals(rule.id())) {
                // Example naming violation check
                checkNamingConvention(context, "Service", violations);
            } else if ("naming.controller-suffix".equals(rule.id())) {
                checkNamingConvention(context, "Controller", violations);
            } else if ("naming.repository-suffix".equals(rule.id())) {
                checkNamingConvention(context, "Repository", violations);
            } else if (rule.id().contains("layering")) {
                // Layer violations from the graph analysis
                if (context.getLayerViolations() != null) {
                    for (LayerDetector.LayerViolation v : context.getLayerViolations()) {
                        Path filePath = getFilePath(context, v.sourceClass());
                        violations.add(new RuleViolation(
                            filePath,
                            1,
                            "Layer violation: " + v.description(),
                            "Inject the target service or dependency rather than accessing it directly."
                        ));
                    }
                }
            } else if ("dependency.field-injection".equals(rule.id()) && rule.confidence() < 0.3) {
                // If constructor injection is highly preferred, field injection is a violation
                checkFieldInjectionViolations(context, violations);
            }
            
            evaluatedRules.add(new EngineeringRule(
                rule.id(),
                rule.category(),
                rule.description(),
                rule.rationale(),
                rule.confidence(),
                rule.supportingExamples(),
                rule.evidence(),
                violations
            ));
        }
        
        return evaluatedRules;
    }
    
    private void checkNamingConvention(AnalysisContext context, String suffix, List<RuleViolation> violations) {
        for (ClassDecl clazz : context.allClasses()) {
            if (clazz.name().toLowerCase().contains(suffix.toLowerCase()) && !clazz.name().endsWith(suffix)) {
                Path filePath = getFilePath(context, clazz.name());
                violations.add(new RuleViolation(
                    filePath,
                    clazz.startLine(),
                    "Class " + clazz.name() + " should end with suffix '" + suffix + "' according to project patterns.",
                    "Rename the class to " + clazz.name() + suffix + " or similar."
                ));
            }
        }
    }
    
    private void checkFieldInjectionViolations(AnalysisContext context, List<RuleViolation> violations) {
        // Field injection violations
        for (ClassDecl clazz : context.allClasses()) {
            Path filePath = getFilePath(context, clazz.name());
            clazz.fields().forEach(f -> {
                f.annotations().forEach(ann -> {
                    if (ann.name().endsWith("Autowired") || ann.name().endsWith("Inject")) {
                        violations.add(new RuleViolation(
                            filePath,
                            f.startLine(),
                            "Field injection is used in field " + f.name() + ". Project prefers constructor injection.",
                            "Use constructor injection instead of @Autowired on fields."
                        ));
                    }
                });
            });
        }
    }
    
    private Path getFilePath(AnalysisContext context, String className) {
        if (context.getParsedFiles() != null) {
            for (ParsedFile pf : context.getParsedFiles()) {
                for (ClassDecl c : pf.classes()) {
                    if (c.name().equals(className) || (pf.packageName() != null && (pf.packageName() + "." + c.name()).equals(className))) {
                        return pf.filePath();
                    }
                }
            }
        }
        return Path.of("unknown");
    }
}
