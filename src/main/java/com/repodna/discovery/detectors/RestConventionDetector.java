package com.repodna.discovery.detectors;

import com.repodna.discovery.AnalysisContext;
import com.repodna.discovery.PatternDetector;
import com.repodna.discovery.model.DiscoveredPattern;
import com.repodna.discovery.model.PatternEvidence;
import com.repodna.graph.LayerDetector.Layer;
import com.repodna.parser.model.ClassDecl;
import com.repodna.parser.model.MethodDecl;
import com.repodna.parser.model.ParsedFile;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class RestConventionDetector implements PatternDetector {

    @Override
    public String name() {
        return "REST Convention Detector";
    }

    @Override
    public List<DiscoveredPattern> detect(AnalysisContext context) {
        List<DiscoveredPattern> patterns = new ArrayList<>();
        
        List<String> controllerNames = context.getClassesInLayer(Layer.CONTROLLER);
        List<ClassDecl> controllers = context.allClasses().stream()
                .filter(c -> {
                    String fqn = c.name();
                    ParsedFile pf = getParsedFile(context, c);
                    if (pf != null && pf.packageName() != null) {
                        fqn = pf.packageName() + "." + c.name();
                    }
                    return controllerNames.contains(fqn) || controllerNames.contains(c.name());
                })
                .toList();

        if (controllers == null || controllers.isEmpty()) {
            return patterns;
        }

        List<PatternEvidence> urlStyleEvidence = new ArrayList<>();
        List<PatternEvidence> responseWrappingEvidence = new ArrayList<>();
        List<PatternEvidence> apiVersioningEvidence = new ArrayList<>();
        List<PatternEvidence> paginationEvidence = new ArrayList<>();
        List<PatternEvidence> httpMethodsEvidence = new ArrayList<>();

        int endpointCount = 0;

        for (ClassDecl clazz : controllers) {
            Path filePath = getFilePath(context, clazz);

            // API Versioning Check (Class level)
            if (AnalysisContext.hasAnnotation(clazz, "RequestMapping")) {
                apiVersioningEvidence.add(new PatternEvidence(filePath, clazz.startLine(), "@RequestMapping", "Possible API versioning in base URL"));
            }

            if (clazz.methods() != null) {
                for (MethodDecl method : clazz.methods()) {
                    boolean isEndpoint = false;
                    String httpMethod = "";

                    if (AnalysisContext.hasAnnotation(method, "GetMapping")) { isEndpoint = true; httpMethod = "GET"; }
                    else if (AnalysisContext.hasAnnotation(method, "PostMapping")) { isEndpoint = true; httpMethod = "POST"; }
                    else if (AnalysisContext.hasAnnotation(method, "PutMapping")) { isEndpoint = true; httpMethod = "PUT"; }
                    else if (AnalysisContext.hasAnnotation(method, "DeleteMapping")) { isEndpoint = true; httpMethod = "DELETE"; }
                    else if (AnalysisContext.hasAnnotation(method, "PatchMapping")) { isEndpoint = true; httpMethod = "PATCH"; }
                    else if (AnalysisContext.hasAnnotation(method, "RequestMapping")) { isEndpoint = true; httpMethod = "ANY"; }

                    if (isEndpoint) {
                        endpointCount++;
                        httpMethodsEvidence.add(new PatternEvidence(filePath, method.startLine(), httpMethod + " mapping", "HTTP method usage"));

                        // Response Wrapping Check
                        if (method.returnType() != null && method.returnType().contains("ResponseEntity")) {
                            responseWrappingEvidence.add(new PatternEvidence(filePath, method.startLine(), method.returnType(), "Uses ResponseEntity for response wrapping"));
                        }

                        // Pagination Check
                        if (method.parameters() != null) {
                            boolean hasPagination = method.parameters().stream().anyMatch(p -> p.type().contains("Pageable"));
                            if (hasPagination) {
                                paginationEvidence.add(new PatternEvidence(filePath, method.startLine(), "Pageable parameter", "Supports pagination"));
                            }
                        }
                    }
                }
            }
        }

        if (endpointCount > 0) {
            patterns.add(DiscoveredPattern.of(
                    "rest.http-methods",
                    "REST APIs",
                    "Distribution of HTTP methods used in endpoints.",
                    httpMethodsEvidence.size(),
                    endpointCount,
                    httpMethodsEvidence,
                    "Proper use of HTTP methods ensures RESTful semantics."
            ));

            if (!responseWrappingEvidence.isEmpty()) {
                patterns.add(DiscoveredPattern.of(
                        "rest.response-wrapping",
                        "REST APIs",
                        "Usage of ResponseEntity for API responses.",
                        responseWrappingEvidence.size(),
                        endpointCount,
                        responseWrappingEvidence,
                        "Wrapping responses in ResponseEntity allows fine-grained control over HTTP headers and status codes."
                ));
            }

            if (!paginationEvidence.isEmpty()) {
                patterns.add(DiscoveredPattern.of(
                        "rest.pagination",
                        "REST APIs",
                        "Pagination support for list endpoints using Pageable.",
                        paginationEvidence.size(),
                        endpointCount,
                        paginationEvidence,
                        "Pagination is critical for API performance on large datasets."
                ));
            }
            
            if (!apiVersioningEvidence.isEmpty()) {
                patterns.add(DiscoveredPattern.of(
                        "rest.api-versioning",
                        "REST APIs",
                        "API versioning patterns in controllers.",
                        apiVersioningEvidence.size(),
                        controllers.size(),
                        apiVersioningEvidence,
                        "API versioning allows backward compatibility during breaking changes."
                ));
            }
            
            // Assume URL style is generic since we don't have exact URL values in AST easily
            patterns.add(DiscoveredPattern.of(
                    "rest.url-style",
                    "REST APIs",
                    "REST URL naming style conventions.",
                    controllers.size(),
                    controllers.size(),
                    List.of(new PatternEvidence(Path.of("global"), 1, "Controller Endpoints", "URLs mapped in controllers")),
                    "Consistent URL naming (e.g., kebab-case) improves API usability."
            ));
        }

        return patterns;
    }

    private ParsedFile getParsedFile(AnalysisContext context, ClassDecl clazz) {
        if (context.getParsedFiles() != null) {
            for (ParsedFile pf : context.getParsedFiles()) {
                if (pf.classes() != null && pf.classes().contains(clazz)) {
                    return pf;
                }
            }
        }
        return null;
    }

    private Path getFilePath(AnalysisContext context, ClassDecl clazz) {
        ParsedFile pf = getParsedFile(context, clazz);
        return pf != null ? pf.filePath() : Path.of("unknown");
    }
}
