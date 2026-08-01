package com.repodna.graph;

import com.repodna.parser.model.*;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import java.util.*;

public class LayerDetector {
    
    public enum Layer {
        CONTROLLER, SERVICE, REPOSITORY, CONFIGURATION, ENTITY, DTO, UTILITY, UNKNOWN
    }
    
    public record LayerAssignment(
        String className,           // FQN
        Layer layer,
        String evidence             // why it was assigned, e.g., "has @RestController annotation"
    ) {}
    
    public record LayerViolation(
        String sourceClass,
        Layer sourceLayer,
        String targetClass,
        Layer targetLayer,
        String description          // e.g., "Controller directly accesses Repository"
    ) {}
    
    /**
     * Assign layers to all classes based on annotations, naming, and package patterns.
     */
    public static List<LayerAssignment> assignLayers(List<ParsedFile> parsedFiles) {
        List<LayerAssignment> assignments = new ArrayList<>();
        
        for (ParsedFile file : parsedFiles) {
            String pkg = file.packageName();
            String prefix = (pkg != null && !pkg.isEmpty()) ? pkg + "." : "";
            
            for (ClassDecl clazz : file.classes()) {
                String fqn = prefix + clazz.name();
                assignments.add(determineLayer(fqn, clazz, pkg));
                
                if (clazz.innerClasses() != null) {
                    for (ClassDecl inner : clazz.innerClasses()) {
                        String innerFqn = fqn + "." + inner.name();
                        assignments.add(determineLayer(innerFqn, inner, pkg));
                    }
                }
            }
        }
        
        return assignments;
    }
    
    private static LayerAssignment determineLayer(String fqn, ClassDecl clazz, String pkg) {
        // 1. CONTROLLER
        if (hasAnnotation(clazz, "RestController") || hasAnnotation(clazz, "Controller")) {
            return new LayerAssignment(fqn, Layer.CONTROLLER, "has Controller annotation");
        }
        if (clazz.name().endsWith("Controller")) {
            return new LayerAssignment(fqn, Layer.CONTROLLER, "name ends with Controller");
        }
        
        // 2. SERVICE
        if (hasAnnotation(clazz, "Service")) {
            return new LayerAssignment(fqn, Layer.SERVICE, "has @Service annotation");
        }
        if (clazz.name().endsWith("Service") || clazz.name().endsWith("ServiceImpl")) {
            return new LayerAssignment(fqn, Layer.SERVICE, "name ends with Service or ServiceImpl");
        }
        
        // 3. REPOSITORY
        if (hasAnnotation(clazz, "Repository")) {
            return new LayerAssignment(fqn, Layer.REPOSITORY, "has @Repository annotation");
        }
        if (clazz.name().endsWith("Repository")) {
            return new LayerAssignment(fqn, Layer.REPOSITORY, "name ends with Repository");
        }
        List<String> interfaces = clazz.interfaces() != null ? clazz.interfaces() : Collections.emptyList();
        for (String iface : interfaces) {
            if (iface.contains("JpaRepository") || iface.contains("CrudRepository") || iface.contains("PagingAndSortingRepository")) {
                return new LayerAssignment(fqn, Layer.REPOSITORY, "extends Spring Data Repository");
            }
        }
        
        // 4. CONFIGURATION
        if (hasAnnotation(clazz, "Configuration")) {
            return new LayerAssignment(fqn, Layer.CONFIGURATION, "has @Configuration annotation");
        }
        if (clazz.name().endsWith("Config") || clazz.name().endsWith("Configuration")) {
            return new LayerAssignment(fqn, Layer.CONFIGURATION, "name ends with Config or Configuration");
        }
        
        // 5. ENTITY
        if (hasAnnotation(clazz, "Entity") || hasAnnotation(clazz, "Table") || hasAnnotation(clazz, "Document")) {
            return new LayerAssignment(fqn, Layer.ENTITY, "has Entity-related annotation");
        }
        if (pkg != null && (pkg.endsWith(".entity") || pkg.endsWith(".model") || pkg.endsWith(".domain") || 
            pkg.contains(".entity.") || pkg.contains(".model.") || pkg.contains(".domain."))) {
            return new LayerAssignment(fqn, Layer.ENTITY, "in entity/model/domain package");
        }
        
        // 6. DTO
        if (clazz.name().endsWith("Dto") || clazz.name().endsWith("DTO") || 
            clazz.name().endsWith("Request") || clazz.name().endsWith("Response") || 
            clazz.name().endsWith("Payload")) {
            return new LayerAssignment(fqn, Layer.DTO, "name matches DTO pattern");
        }
        if (clazz.classType() != null && clazz.classType().name().equals("RECORD") && 
            pkg != null && (pkg.endsWith(".dto") || pkg.contains(".dto."))) {
            return new LayerAssignment(fqn, Layer.DTO, "is a record in dto package");
        }
        
        // 7. UTILITY
        if (clazz.name().endsWith("Util") || clazz.name().endsWith("Utils") || 
            clazz.name().endsWith("Helper") || clazz.name().endsWith("Constants")) {
            return new LayerAssignment(fqn, Layer.UTILITY, "name matches Utility pattern");
        }
        
        // 8. UNKNOWN
        return new LayerAssignment(fqn, Layer.UNKNOWN, "default assignment");
    }
    
    private static boolean hasAnnotation(ClassDecl clazz, String annotationName) {
        if (clazz.annotations() == null) return false;
        for (AnnotationDecl ann : clazz.annotations()) {
            if (ann.name().equals(annotationName) || ann.name().endsWith("." + annotationName)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Detect layer violations (e.g., Controller -> Repository direct access).
     * Uses the class dependency graph and layer assignments.
     */
    public static List<LayerViolation> detectViolations(
            Map<String, Layer> layerMap,
            DefaultDirectedGraph<String, DefaultEdge> classGraph) {
        
        List<LayerViolation> violations = new ArrayList<>();
        
        for (DefaultEdge edge : classGraph.edgeSet()) {
            String sourceFqn = classGraph.getEdgeSource(edge);
            String targetFqn = classGraph.getEdgeTarget(edge);
            
            Layer sourceLayer = layerMap.getOrDefault(sourceFqn, Layer.UNKNOWN);
            Layer targetLayer = layerMap.getOrDefault(targetFqn, Layer.UNKNOWN);
            
            if (sourceLayer == Layer.UNKNOWN || targetLayer == Layer.UNKNOWN) {
                continue;
            }
            
            // CONTROLLER -> REPOSITORY is a violation (should go through SERVICE)
            if (sourceLayer == Layer.CONTROLLER && targetLayer == Layer.REPOSITORY) {
                violations.add(new LayerViolation(sourceFqn, sourceLayer, targetFqn, targetLayer, 
                    "Controller directly accesses Repository"));
            }
            
            // REPOSITORY -> CONTROLLER is a violation
            if (sourceLayer == Layer.REPOSITORY && targetLayer == Layer.CONTROLLER) {
                violations.add(new LayerViolation(sourceFqn, sourceLayer, targetFqn, targetLayer, 
                    "Repository accesses Controller (reverse dependency)"));
            }
            
            // REPOSITORY -> SERVICE is a violation (reverse dependency)
            if (sourceLayer == Layer.REPOSITORY && targetLayer == Layer.SERVICE) {
                violations.add(new LayerViolation(sourceFqn, sourceLayer, targetFqn, targetLayer, 
                    "Repository accesses Service (reverse dependency)"));
            }
            
            // ENTITY -> SERVICE is a violation
            if (sourceLayer == Layer.ENTITY && targetLayer == Layer.SERVICE) {
                violations.add(new LayerViolation(sourceFqn, sourceLayer, targetFqn, targetLayer, 
                    "Entity accesses Service"));
            }
            
            // ENTITY -> CONTROLLER is a violation
            if (sourceLayer == Layer.ENTITY && targetLayer == Layer.CONTROLLER) {
                violations.add(new LayerViolation(sourceFqn, sourceLayer, targetFqn, targetLayer, 
                    "Entity accesses Controller"));
            }
            
            // DTO -> REPOSITORY is a violation
            if (sourceLayer == Layer.DTO && targetLayer == Layer.REPOSITORY) {
                violations.add(new LayerViolation(sourceFqn, sourceLayer, targetFqn, targetLayer, 
                    "DTO accesses Repository"));
            }
        }
        
        return violations;
    }
}
