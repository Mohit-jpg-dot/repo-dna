package com.repodna.discovery.detectors;

import com.repodna.discovery.AnalysisContext;
import com.repodna.discovery.PatternDetector;
import com.repodna.discovery.model.DiscoveredPattern;
import com.repodna.discovery.model.PatternEvidence;
import com.repodna.parser.model.ClassDecl;
import com.repodna.parser.model.MethodDecl;
import com.repodna.parser.model.ParsedFile;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class SecurityPatternDetector implements PatternDetector {

    @Override
    public String name() {
        return "Security Pattern Detector";
    }

    @Override
    public List<DiscoveredPattern> detect(AnalysisContext context) {
        List<DiscoveredPattern> patterns = new ArrayList<>();
        List<ClassDecl> allClasses = context.allClasses();
        if (allClasses == null || allClasses.isEmpty()) {
            return patterns;
        }

        List<PatternEvidence> methodSecurityEvidence = new ArrayList<>();
        List<PatternEvidence> securityConfigEvidence = new ArrayList<>();
        List<PatternEvidence> corsConfigEvidence = new ArrayList<>();
        List<PatternEvidence> authPrincipalEvidence = new ArrayList<>();

        int configCount = 0;
        int securedMethodCount = 0;

        for (ClassDecl clazz : allClasses) {
            Path filePath = getFilePath(context, clazz);

            // Check for Security Config
            if (AnalysisContext.hasAnnotation(clazz, "EnableWebSecurity") || 
                (clazz.superClass() != null && clazz.superClass().contains("WebSecurityConfigurerAdapter"))) {
                configCount++;
                securityConfigEvidence.add(new PatternEvidence(filePath, clazz.startLine(), "class " + clazz.name(), "Spring Security Configuration found"));
            }

            // Check for CORS
            if (AnalysisContext.hasAnnotation(clazz, "CrossOrigin")) {
                corsConfigEvidence.add(new PatternEvidence(filePath, clazz.startLine(), "@CrossOrigin", "CORS Configuration found"));
            }

            if (clazz.methods() != null) {
                for (MethodDecl method : clazz.methods()) {
                    if (AnalysisContext.hasAnnotation(method, "PreAuthorize") || AnalysisContext.hasAnnotation(method, "Secured") || AnalysisContext.hasAnnotation(method, "RolesAllowed")) {
                        securedMethodCount++;
                        methodSecurityEvidence.add(new PatternEvidence(filePath, method.startLine(), method.name(), "Method level security applied"));
                    }

                    if (AnalysisContext.hasAnnotation(method, "CrossOrigin")) {
                        corsConfigEvidence.add(new PatternEvidence(filePath, method.startLine(), "@CrossOrigin", "CORS applied on method"));
                    }

                    if (method.parameters() != null) {
                        boolean hasAuthPrincipal = method.parameters().stream().anyMatch(p -> p.type().contains("AuthenticationPrincipal") || p.name().contains("Principal"));
                        if (hasAuthPrincipal) {
                            authPrincipalEvidence.add(new PatternEvidence(filePath, method.startLine(), method.name(), "Uses @AuthenticationPrincipal"));
                        }
                    }
                }
            }
        }

        if (!methodSecurityEvidence.isEmpty()) {
            patterns.add(DiscoveredPattern.of(
                    "security.method-security",
                    "Security",
                    "Method-level security annotations (@PreAuthorize, @Secured).",
                    methodSecurityEvidence.size(),
                    methodSecurityEvidence.size(),
                    methodSecurityEvidence,
                    "Method-level security ensures fine-grained access control."
            ));
        }

        if (!securityConfigEvidence.isEmpty()) {
            patterns.add(DiscoveredPattern.of(
                    "security.security-config",
                    "Security",
                    "Spring Security configuration classes.",
                    configCount,
                    configCount,
                    securityConfigEvidence,
                    "Centralized security configurations handle global authentication and authorization."
            ));
        }

        if (!corsConfigEvidence.isEmpty()) {
            patterns.add(DiscoveredPattern.of(
                    "security.cors-config",
                    "Security",
                    "CORS configuration presence.",
                    corsConfigEvidence.size(),
                    corsConfigEvidence.size(),
                    corsConfigEvidence,
                    "Explicit CORS configuration helps manage cross-origin request safety."
            ));
        }

        if (!authPrincipalEvidence.isEmpty()) {
            patterns.add(DiscoveredPattern.of(
                    "security.auth-principal",
                    "Security",
                    "Usage of @AuthenticationPrincipal for user resolution.",
                    authPrincipalEvidence.size(),
                    authPrincipalEvidence.size(),
                    authPrincipalEvidence,
                    "Resolving the authenticated principal cleanly decouples context from business logic."
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
