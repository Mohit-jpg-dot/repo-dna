package com.repodna.discovery;

import com.repodna.parser.model.*;
import com.repodna.graph.*;

import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import java.util.List;
import java.util.Map;

public class AnalysisContext {
    private final Object projectInfo; // Using Object to avoid dependency issues if not present
    private final List<Object> sourceFiles; 
    private final List<ParsedFile> parsedFiles;
    private final DefaultDirectedGraph<String, DefaultEdge> classGraph;
    private final DefaultDirectedGraph<String, DefaultEdge> packageGraph;
    private final Map<String, LayerDetector.Layer> layerMap;
    private final List<LayerDetector.LayerViolation> layerViolations;
    
    public AnalysisContext(
            Object projectInfo,
            List<Object> sourceFiles,
            List<ParsedFile> parsedFiles,
            DefaultDirectedGraph<String, DefaultEdge> classGraph,
            DefaultDirectedGraph<String, DefaultEdge> packageGraph,
            Map<String, LayerDetector.Layer> layerMap,
            List<LayerDetector.LayerViolation> layerViolations) {
        this.projectInfo = projectInfo;
        this.sourceFiles = sourceFiles;
        this.parsedFiles = parsedFiles;
        this.classGraph = classGraph;
        this.packageGraph = packageGraph;
        this.layerMap = layerMap;
        this.layerViolations = layerViolations;
    }

    public Object getProjectInfo() { return projectInfo; }
    public List<Object> getSourceFiles() { return sourceFiles; }
    public List<ParsedFile> getParsedFiles() { return parsedFiles; }
    public DefaultDirectedGraph<String, DefaultEdge> getClassGraph() { return classGraph; }
    public DefaultDirectedGraph<String, DefaultEdge> getPackageGraph() { return packageGraph; }
    public Map<String, LayerDetector.Layer> getLayerMap() { return layerMap; }
    public List<LayerDetector.LayerViolation> getLayerViolations() { return layerViolations; }
    
    /** Get all parsed classes across all files */
    public List<ClassDecl> allClasses() {
        return parsedFiles.stream()
                .flatMap(f -> f.classes().stream())
                .toList();
    }
    
    /** Get all methods across all classes */
    public List<MethodDecl> allMethods() {
        return allClasses().stream()
                .flatMap(c -> c.methods().stream())
                .toList();
    }
    
    /** Get classes by layer */
    public List<String> getClassesInLayer(LayerDetector.Layer layer) {
        return layerMap.entrySet().stream()
                .filter(e -> e.getValue() == layer)
                .map(Map.Entry::getKey)
                .toList();
    }
    
    /** Get source (non-test) files */
    public List<ParsedFile> sourceOnlyFiles() {
        return parsedFiles.stream()
                .filter(f -> !f.filePath().toString().contains("/test/"))
                .toList();
    }
    
    /** Check if a class has a specific annotation */
    public static boolean hasAnnotation(ClassDecl cls, String annotationName) {
        return cls.annotations() != null && cls.annotations().stream()
                .anyMatch(a -> a.name().equals(annotationName) || a.name().endsWith("." + annotationName));
    }
    
    /** Check if a method has a specific annotation */
    public static boolean hasAnnotation(MethodDecl method, String annotationName) {
        return method.annotations() != null && method.annotations().stream()
                .anyMatch(a -> a.name().equals(annotationName) || a.name().endsWith("." + annotationName));
    }
}
