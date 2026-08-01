package com.repodna.discovery.detectors;

import com.repodna.discovery.AnalysisContext;
import com.repodna.discovery.PatternDetector;
import com.repodna.discovery.model.DiscoveredPattern;
import com.repodna.discovery.model.PatternEvidence;
import com.repodna.parser.model.AnnotationDecl;
import com.repodna.parser.model.ClassDecl;
import com.repodna.parser.model.FieldDecl;
import com.repodna.parser.model.MethodDecl;
import com.repodna.parser.model.ParsedFile;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Detects dependency injection and boundary patterns in the application.
 */
public class DependencyBoundaryDetector implements PatternDetector {
    @Override
    public String name() {
        return "Dependency Boundary Detector";
    }

    @Override
    public List<DiscoveredPattern> detect(AnalysisContext context) {
        List<DiscoveredPattern> patterns = new ArrayList<>();

        List<PatternEvidence> fieldInjEvidence = new ArrayList<>();
        List<PatternEvidence> constructorInjEvidence = new ArrayList<>();
        List<PatternEvidence> lombokInjEvidence = new ArrayList<>();
        
        int fieldInjCount = 0;
        int constructorInjCount = 0;
        int lombokInjCount = 0;
        int totalSpringBeans = 0;

        for (ClassDecl clazz : context.allClasses()) {
            Path filePath = getFilePath(context, clazz);
            boolean isBean = false;
            for (AnnotationDecl ann : clazz.annotations()) {
                if (ann.name().endsWith("Service") || ann.name().endsWith("Component") ||
                    ann.name().endsWith("Repository") || ann.name().endsWith("Controller") || 
                    ann.name().endsWith("RestController") || ann.name().endsWith("Configuration")) {
                    isBean = true;
                    break;
                }
            }

            if (isBean) {
                totalSpringBeans++;
                
                boolean hasFieldInj = false;
                for (FieldDecl field : clazz.fields()) {
                    for (AnnotationDecl ann : field.annotations()) {
                        if (ann.name().endsWith("Autowired") || ann.name().endsWith("Inject") || ann.name().endsWith("Resource")) {
                            hasFieldInj = true;
                            fieldInjEvidence.add(new PatternEvidence(filePath, field.startLine(),
                                    field.type() + " " + field.name(), "Field injection used"));
                        }
                    }
                }
                if (hasFieldInj) fieldInjCount++;

                boolean hasLombokInj = false;
                for (AnnotationDecl ann : clazz.annotations()) {
                    if (ann.name().endsWith("RequiredArgsConstructor") || ann.name().endsWith("AllArgsConstructor")) {
                        hasLombokInj = true;
                        lombokInjEvidence.add(new PatternEvidence(filePath, clazz.startLine(),
                                "@" + ann.name(), "Lombok-based constructor injection"));
                    }
                }
                if (hasLombokInj) lombokInjCount++;

                boolean hasExplicitConstructorInj = false;
                for (MethodDecl method : clazz.methods()) {
                    // Check if it's a constructor with parameters
                    if (method.isConstructor() && !method.parameters().isEmpty()) {
                        hasExplicitConstructorInj = true;
                        constructorInjEvidence.add(new PatternEvidence(filePath, method.startLine(),
                                method.name() + "(...)", "Explicit constructor injection"));
                    }
                }
                if (hasExplicitConstructorInj) constructorInjCount++;
            }
        }

        if (fieldInjCount > 0) {
            patterns.add(DiscoveredPattern.of(
                    "dependency.field-injection",
                    "Dependency Injection",
                    "Usage of field-based dependency injection.",
                    fieldInjCount,
                    totalSpringBeans,
                    fieldInjEvidence,
                    fieldInjCount + " out of " + totalSpringBeans + " beans use field injection."
            ));
        }

        if (constructorInjCount > 0) {
            patterns.add(DiscoveredPattern.of(
                    "dependency.constructor-injection",
                    "Dependency Injection",
                    "Usage of explicit constructor-based dependency injection.",
                    constructorInjCount,
                    totalSpringBeans,
                    constructorInjEvidence,
                    constructorInjCount + " out of " + totalSpringBeans + " beans use explicit constructor injection."
            ));
        }

        if (lombokInjCount > 0) {
            patterns.add(DiscoveredPattern.of(
                    "dependency.lombok-injection",
                    "Dependency Injection",
                    "Usage of Lombok-based constructor injection (@RequiredArgsConstructor or @AllArgsConstructor).",
                    lombokInjCount,
                    totalSpringBeans,
                    lombokInjEvidence,
                    lombokInjCount + " out of " + totalSpringBeans + " beans use Lombok constructor injection."
            ));
        }

        return patterns;
    }

    private Path getFilePath(AnalysisContext context, ClassDecl clazz) {
        if (context.getParsedFiles() != null) {
            for (ParsedFile pf : context.getParsedFiles()) {
                if (pf.classes() != null && pf.classes().contains(clazz)) {
                    return pf.filePath();
                }
            }
        }
        return Path.of("unknown");
    }
}
