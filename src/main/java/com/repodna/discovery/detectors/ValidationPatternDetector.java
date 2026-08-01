package com.repodna.discovery.detectors;

import com.repodna.discovery.AnalysisContext;
import com.repodna.discovery.PatternDetector;
import com.repodna.discovery.model.DiscoveredPattern;
import com.repodna.discovery.model.PatternEvidence;
import com.repodna.parser.model.ClassDecl;
import com.repodna.parser.model.MethodDecl;
import com.repodna.parser.model.ParsedFile;
import com.repodna.graph.LayerDetector.Layer;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ValidationPatternDetector implements PatternDetector {

    private static final List<String> VALIDATION_ANNOTATIONS = List.of(
            "Valid", "NotNull", "NotBlank", "NotEmpty", "Size", "Min", "Max", "Email", "Pattern"
    );

    @Override
    public String name() {
        return "Validation Pattern Detector";
    }

    @Override
    public List<DiscoveredPattern> detect(AnalysisContext context) {
        List<DiscoveredPattern> patterns = new ArrayList<>();
        List<ClassDecl> allClasses = context.allClasses();
        if (allClasses == null || allClasses.isEmpty()) {
            return patterns;
        }

        List<PatternEvidence> beanValidationEvidence = new ArrayList<>();
        List<PatternEvidence> controllerValidationEvidence = new ArrayList<>();
        List<PatternEvidence> serviceValidationEvidence = new ArrayList<>();
        List<PatternEvidence> customValidatorEvidence = new ArrayList<>();

        int totalValidationOpportunities = 0;
        int customValidatorCount = 0;

        for (ClassDecl clazz : allClasses) {
            Path filePath = getFilePath(context, clazz);

            // Check for custom validators
            if (AnalysisContext.hasAnnotation(clazz, "Constraint") || 
                (clazz.name() != null && clazz.name().endsWith("Validator"))) {
                customValidatorCount++;
                customValidatorEvidence.add(new PatternEvidence(filePath, clazz.startLine(), "class " + clazz.name(), "Custom constraint validator found"));
            }

            boolean isController = context.getClassesInLayer(Layer.CONTROLLER) != null && context.getClassesInLayer(Layer.CONTROLLER).contains(clazz);
            boolean isService = context.getClassesInLayer(Layer.SERVICE) != null && context.getClassesInLayer(Layer.SERVICE).contains(clazz);

            if (clazz.methods() != null) {
                for (MethodDecl method : clazz.methods()) {
                    boolean hasValidation = false;
                    for (String ann : VALIDATION_ANNOTATIONS) {
                        if (AnalysisContext.hasAnnotation(method, ann) || method.parameters().stream().anyMatch(p -> p.type().contains(ann) || p.name().contains(ann))) { // Simplification
                            hasValidation = true;
                            beanValidationEvidence.add(new PatternEvidence(filePath, method.startLine(), method.name(), "Uses Bean Validation annotation @" + ann));
                            break;
                        }
                    }

                    if (hasValidation) {
                        totalValidationOpportunities++;
                        if (isController) {
                            controllerValidationEvidence.add(new PatternEvidence(filePath, method.startLine(), method.name(), "Validation applied at Controller level"));
                        } else if (isService) {
                            serviceValidationEvidence.add(new PatternEvidence(filePath, method.startLine(), method.name(), "Validation applied at Service level"));
                        }
                    }
                }
            }
        }

        if (!beanValidationEvidence.isEmpty()) {
            patterns.add(DiscoveredPattern.of(
                    "validation.bean-validation",
                    "Validation",
                    "Usage of standard Bean Validation annotations.",
                    beanValidationEvidence.size(),
                    totalValidationOpportunities > 0 ? totalValidationOpportunities : beanValidationEvidence.size(),
                    beanValidationEvidence,
                    "Standard bean validation ensures consistent constraint checking."
            ));
        }

        if (!controllerValidationEvidence.isEmpty()) {
            patterns.add(DiscoveredPattern.of(
                    "validation.controller-level",
                    "Validation",
                    "Validation enforced at the Controller layer.",
                    controllerValidationEvidence.size(),
                    totalValidationOpportunities,
                    controllerValidationEvidence,
                    "Validating early in the controller layer prevents bad data from entering business logic."
            ));
        }

        if (!serviceValidationEvidence.isEmpty()) {
            patterns.add(DiscoveredPattern.of(
                    "validation.service-level",
                    "Validation",
                    "Validation enforced at the Service layer.",
                    serviceValidationEvidence.size(),
                    totalValidationOpportunities,
                    serviceValidationEvidence,
                    "Validating in the service layer ensures business rules are maintained regardless of the entry point."
            ));
        }

        if (customValidatorCount > 0) {
            patterns.add(DiscoveredPattern.of(
                    "validation.custom-validators",
                    "Validation",
                    "Custom constraint validators implementation.",
                    customValidatorCount,
                    customValidatorCount,
                    customValidatorEvidence,
                    "Custom validators encapsulate complex validation rules cleanly."
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
