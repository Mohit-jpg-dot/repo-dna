package com.repodna.graph;

import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import com.repodna.parser.model.*;
import java.util.*;

public class DependencyGraphBuilder {
    private final List<ParsedFile> parsedFiles;
    
    // Maps for resolving references
    private final Map<String, List<String>> simpleNameToFqns;  // "UserService" -> ["com.example.service.UserService"]
    private final Map<String, ParsedFile> fqnToFile;            // FQN -> parsed file
    
    public DependencyGraphBuilder(List<ParsedFile> parsedFiles) {
        this.parsedFiles = parsedFiles;
        this.simpleNameToFqns = new HashMap<>();
        this.fqnToFile = new HashMap<>();
        
        for (ParsedFile file : parsedFiles) {
            String pkg = file.packageName();
            String prefix = (pkg != null && !pkg.isEmpty()) ? pkg + "." : "";
            for (ClassDecl clazz : file.classes()) {
                String fqn = prefix + clazz.name();
                simpleNameToFqns.computeIfAbsent(clazz.name(), k -> new ArrayList<>()).add(fqn);
                fqnToFile.put(fqn, file);
                
                if (clazz.innerClasses() != null) {
                    for (ClassDecl inner : clazz.innerClasses()) {
                        String innerFqn = fqn + "." + inner.name();
                        simpleNameToFqns.computeIfAbsent(inner.name(), k -> new ArrayList<>()).add(innerFqn);
                        fqnToFile.put(innerFqn, file);
                    }
                }
            }
        }
    }
    
    /**
     * Build class dependency graph.
     * Nodes are fully qualified class names.
     * Edges represent dependencies (field types, method params, return types, imports).
     */
    public DefaultDirectedGraph<String, DefaultEdge> buildClassGraph() {
        DefaultDirectedGraph<String, DefaultEdge> graph = new DefaultDirectedGraph<>(DefaultEdge.class);
        
        for (String fqn : fqnToFile.keySet()) {
            graph.addVertex(fqn);
        }
        
        for (Map.Entry<String, ParsedFile> entry : fqnToFile.entrySet()) {
            String sourceFqn = entry.getKey();
            ParsedFile file = entry.getValue();
            
            ClassDecl sourceClass = null;
            for (ClassDecl clazz : file.classes()) {
                String prefix = (file.packageName() != null && !file.packageName().isEmpty()) ? file.packageName() + "." : "";
                if ((prefix + clazz.name()).equals(sourceFqn)) {
                    sourceClass = clazz;
                    break;
                }
                if (clazz.innerClasses() != null) {
                    for (ClassDecl inner : clazz.innerClasses()) {
                        if ((prefix + clazz.name() + "." + inner.name()).equals(sourceFqn)) {
                            sourceClass = inner;
                            break;
                        }
                    }
                }
                if (sourceClass != null) break;
            }
            if (sourceClass == null) continue;
            
            Set<String> dependencies = new HashSet<>();
            
            // imports
            if (file.imports() != null) {
                for (ImportDecl imp : file.imports()) {
                    String qName = imp.qualifiedName();
                    if (fqnToFile.containsKey(qName)) {
                        dependencies.add(qName);
                    }
                }
            }
            
            // superclass
            if (sourceClass.superClass() != null) {
                String resolved = resolveDependency(sourceClass.superClass(), file);
                if (resolved != null) dependencies.add(resolved);
            }
            
            // interfaces
            if (sourceClass.interfaces() != null) {
                for (String iface : sourceClass.interfaces()) {
                    String resolved = resolveDependency(iface, file);
                    if (resolved != null) dependencies.add(resolved);
                }
            }
            
            // fields
            if (sourceClass.fields() != null) {
                for (FieldDecl field : sourceClass.fields()) {
                    String resolved = resolveDependency(field.type(), file);
                    if (resolved != null) dependencies.add(resolved);
                }
            }
            
            // methods
            if (sourceClass.methods() != null) {
                for (MethodDecl method : sourceClass.methods()) {
                    String resolvedRet = resolveDependency(method.returnType(), file);
                    if (resolvedRet != null) dependencies.add(resolvedRet);
                    
                    if (method.parameters() != null) {
                        for (MethodDecl.ParameterInfo param : method.parameters()) {
                            String resolvedParam = resolveDependency(param.type(), file);
                            if (resolvedParam != null) dependencies.add(resolvedParam);
                        }
                    }
                }
            }
            
            for (String targetFqn : dependencies) {
                if (!sourceFqn.equals(targetFqn) && graph.containsVertex(targetFqn)) {
                    graph.addEdge(sourceFqn, targetFqn);
                }
            }
        }
        
        return graph;
    }
    
    /**
     * Build package dependency graph.
     * Nodes are package names.
     * Edges represent package-level dependencies (aggregated from class graph).
     */
    public DefaultDirectedGraph<String, DefaultEdge> buildPackageGraph() {
        DefaultDirectedGraph<String, DefaultEdge> packageGraph = new DefaultDirectedGraph<>(DefaultEdge.class);
        DefaultDirectedGraph<String, DefaultEdge> classGraph = buildClassGraph();
        
        Set<String> packages = new HashSet<>();
        for (String fqn : classGraph.vertexSet()) {
            String pkg = getPackageName(fqn);
            if (pkg != null && !pkg.isEmpty()) {
                packages.add(pkg);
                packageGraph.addVertex(pkg);
            }
        }
        
        for (DefaultEdge edge : classGraph.edgeSet()) {
            String source = classGraph.getEdgeSource(edge);
            String target = classGraph.getEdgeTarget(edge);
            
            String sourcePkg = getPackageName(source);
            String targetPkg = getPackageName(target);
            
            if (sourcePkg != null && targetPkg != null && !sourcePkg.equals(targetPkg)) {
                if (!packageGraph.containsEdge(sourcePkg, targetPkg)) {
                    packageGraph.addEdge(sourcePkg, targetPkg);
                }
            }
        }
        
        return packageGraph;
    }
    
    private String resolveDependency(String typeRef, ParsedFile context) {
        if (typeRef == null) return null;
        
        // Strip array brackets and generics for basic matching
        String baseType = typeRef.replaceAll("<.*>", "").replaceAll("\\[\\]", "").trim();
        
        // Is it already FQN in project?
        if (fqnToFile.containsKey(baseType)) {
            return baseType;
        }
        
        // Check explicit imports (e.g. import com.example.admin.User;)
        if (context.imports() != null) {
            for (ImportDecl imp : context.imports()) {
                if (imp.qualifiedName().endsWith("." + baseType)) {
                    if (fqnToFile.containsKey(imp.qualifiedName())) {
                        return imp.qualifiedName();
                    }
                }
            }
        }
        
        // Check same package
        String contextPkg = context.packageName();
        String samePkgFqn = (contextPkg != null && !contextPkg.isEmpty()) ? contextPkg + "." + baseType : baseType;
        if (fqnToFile.containsKey(samePkgFqn)) {
            return samePkgFqn;
        }
        
        // Check wildcard imports (e.g. import com.example.admin.*;)
        if (context.imports() != null) {
            for (ImportDecl imp : context.imports()) {
                if (imp.isWildcard()) {
                    String possibleFqn = imp.qualifiedName() + "." + baseType;
                    if (fqnToFile.containsKey(possibleFqn)) {
                        return possibleFqn;
                    }
                }
            }
        }
        
        // Check global simple name mappings
        List<String> matches = simpleNameToFqns.get(baseType);
        if (matches != null && !matches.isEmpty()) {
            if (matches.size() == 1) {
                return matches.get(0);
            }
            for (String fqn : matches) {
                if (contextPkg != null && fqn.startsWith(contextPkg + ".")) {
                    return fqn;
                }
            }
            return matches.get(0);
        }
        
        return null;
    }
    
    private String getPackageName(String fqn) {
        int lastDot = fqn.lastIndexOf('.');
        return (lastDot > 0) ? fqn.substring(0, lastDot) : "";
    }
}
