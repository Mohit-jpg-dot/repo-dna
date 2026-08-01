package com.repodna.graph;

import com.repodna.graph.model.*;
import com.repodna.parser.SpringDetector;
import com.repodna.parser.model.*;
import com.repodna.scanner.model.ScannerResult;

import java.util.*;

/**
 * Factory class that populates the Repository Knowledge Graph (RKG) from scanner and parser results.
 */
public class GraphBuilder {

    /**
     * Translates scanner results and Java parsed models into a queryable knowledge graph.
     */
    public static RepositoryKnowledgeGraph build(ScannerResult scannerResult, ProjectModel projectModel) {
        RepositoryKnowledgeGraph graph = new RepositoryKnowledgeGraph();

        // 1. Create Root Repository Node
        Map<String, Object> repoMeta = new HashMap<>();
        repoMeta.put("buildSystem", scannerResult.buildSystem().name());
        repoMeta.put("packageManager", scannerResult.buildSystem().packageManager());
        RkgNode repoNode = new RkgNode(
            scannerResult.repoName(),
            RkgNodeType.REPOSITORY,
            scannerResult.repoPath().toString(),
            repoMeta,
            1
        );
        graph.addNode(repoNode);

        // 2. Add Documentation, Build, and Configuration Nodes
        for (String doc : scannerResult.documentation().presentDocs()) {
            RkgNode docNode = new RkgNode(doc, RkgNodeType.DOCUMENTATION_FILE, doc, Collections.emptyMap(), 1);
            graph.addNode(docNode);
            graph.addEdge(new RkgEdge(repoNode.id(), docNode.id(), RkgEdgeType.CONTAINS, 1.0, Collections.emptyList(), null));
        }

        for (String buildFile : scannerResult.buildSystem().buildFiles()) {
            RkgNode buildNode = new RkgNode(buildFile, RkgNodeType.BUILD_FILE, buildFile, Collections.emptyMap(), 1);
            graph.addNode(buildNode);
            graph.addEdge(new RkgEdge(repoNode.id(), buildNode.id(), RkgEdgeType.CONTAINS, 1.0, Collections.emptyList(), null));
        }

        for (String config : scannerResult.configs()) {
            RkgNode configNode = new RkgNode(config, RkgNodeType.CONFIGURATION_FILE, config, Collections.emptyMap(), 1);
            graph.addNode(configNode);
            graph.addEdge(new RkgEdge(repoNode.id(), configNode.id(), RkgEdgeType.CONTAINS, 1.0, Collections.emptyList(), null));
        }

        // 3. Add Git Metadata Node
        if (scannerResult.gitInfo().isGitRepo()) {
            Map<String, Object> gitMeta = new HashMap<>();
            gitMeta.put("branch", scannerResult.gitInfo().currentBranch());
            gitMeta.put("commits", scannerResult.gitInfo().commitCount());
            gitMeta.put("tags", scannerResult.gitInfo().tagCount());
            gitMeta.put("lastCommit", scannerResult.gitInfo().lastCommitTimestamp());
            RkgNode gitNode = new RkgNode("git-metadata", RkgNodeType.GIT_METADATA, ".git", gitMeta, 1);
            graph.addNode(gitNode);
            graph.addEdge(new RkgEdge(repoNode.id(), gitNode.id(), RkgEdgeType.CONTAINS, 1.0, Collections.emptyList(), null));
        }

        // 4. Populate Java Package and Class Hierarchies
        for (PackageModel pkg : projectModel.packages()) {
            Map<String, Object> pkgMeta = new HashMap<>();
            pkgMeta.put("classesCount", pkg.classes().size());
            RkgNode pkgNode = new RkgNode(pkg.name(), RkgNodeType.PACKAGE, pkg.name(), pkgMeta, 1);
            graph.addNode(pkgNode);
            graph.addEdge(new RkgEdge(repoNode.id(), pkgNode.id(), RkgEdgeType.CONTAINS, 1.0, Collections.emptyList(), null));

            // Class nodes
            for (ClassModel cm : pkg.classes()) {
                RkgNode classNode = buildClassNode(cm, pkg.name());
                graph.addNode(classNode);
                graph.addEdge(new RkgEdge(pkgNode.id(), classNode.id(), RkgEdgeType.CONTAINS, 1.0, Collections.emptyList(), null));

                // Process inheritance and interfaces
                if (cm.superClass() != null) {
                    graph.addEdge(new RkgEdge(classNode.id(), cm.superClass().qualifiedName(), RkgEdgeType.EXTENDS, 1.0, Collections.emptyList(), null));
                    graph.addEdge(new RkgEdge(classNode.id(), cm.superClass().qualifiedName(), RkgEdgeType.DEPENDS_ON, 1.0, Collections.emptyList(), null));
                }
                for (TypeReference intr : cm.interfaces()) {
                    graph.addEdge(new RkgEdge(classNode.id(), intr.qualifiedName(), RkgEdgeType.IMPLEMENTS, 1.0, Collections.emptyList(), null));
                    graph.addEdge(new RkgEdge(classNode.id(), intr.qualifiedName(), RkgEdgeType.DEPENDS_ON, 1.0, Collections.emptyList(), null));
                }

                // Spring Boot injection style and component mapping
                SpringDetector.SpringMeta springMeta = SpringDetector.detect(cm);
                if (springMeta.isSpringManaged()) {
                    Map<String, Object> frameworkMeta = new HashMap<>();
                    frameworkMeta.put("stereotypes", springMeta.stereotypes());
                    frameworkMeta.put("injectionStyle", springMeta.injectionStyle());
                    frameworkMeta.put("endpoints", springMeta.endpoints());
                    RkgNode componentNode = new RkgNode(
                        classNode.id() + "$SpringBean",
                        RkgNodeType.FRAMEWORK_COMPONENT,
                        classNode.sourceLocation(),
                        frameworkMeta,
                        1
                    );
                    graph.addNode(componentNode);
                    graph.addEdge(new RkgEdge(classNode.id(), componentNode.id(), RkgEdgeType.CONTAINS, 1.0, Collections.emptyList(), null));
                }

                // Member methods
                for (MethodModel mm : cm.methods()) {
                    RkgNode methodNode = buildMethodNode(mm, classNode.id(), pkg.name());
                    graph.addNode(methodNode);
                    graph.addEdge(new RkgEdge(classNode.id(), methodNode.id(), RkgEdgeType.CONTAINS, 1.0, Collections.emptyList(), null));

                    // Method calls -> Edges
                    for (CallSiteModel call : mm.calls()) {
                        if (!"unknown".equals(call.calleeTypeFqn())) {
                            String targetMethodId = call.calleeTypeFqn() + "::" + call.methodName();
                            graph.addEdge(new RkgEdge(methodNode.id(), targetMethodId, RkgEdgeType.CALLS, 1.0, List.of("Line " + call.line()), null));
                            
                            // Map dependency edge between classes
                            if (!classNode.id().equals(call.calleeTypeFqn())) {
                                graph.addEdge(new RkgEdge(classNode.id(), call.calleeTypeFqn(), RkgEdgeType.DEPENDS_ON, 1.0, List.of("Invoked method " + call.methodName()), null));
                            }
                        }
                    }
                }
            }

            // Interface nodes
            for (InterfaceModel im : pkg.interfaces()) {
                Map<String, Object> imMeta = new HashMap<>();
                imMeta.put("package", pkg.name());
                imMeta.put("sourceFile", im.sourceFile());
                RkgNode intNode = new RkgNode(im.qualifiedName(), RkgNodeType.INTERFACE, im.sourceFile(), imMeta, 1);
                graph.addNode(intNode);
                graph.addEdge(new RkgEdge(pkgNode.id(), intNode.id(), RkgEdgeType.CONTAINS, 1.0, Collections.emptyList(), null));
            }

            // Enum nodes
            for (EnumModel em : pkg.enums()) {
                Map<String, Object> emMeta = new HashMap<>();
                emMeta.put("package", pkg.name());
                emMeta.put("sourceFile", em.sourceFile());
                emMeta.put("constants", em.entries());
                RkgNode enumNode = new RkgNode(em.qualifiedName(), RkgNodeType.ENUM, em.sourceFile(), emMeta, 1);
                graph.addNode(enumNode);
                graph.addEdge(new RkgEdge(pkgNode.id(), enumNode.id(), RkgEdgeType.CONTAINS, 1.0, Collections.emptyList(), null));
            }

            // Record nodes
            for (RecordModel rm : pkg.records()) {
                Map<String, Object> rmMeta = new HashMap<>();
                rmMeta.put("package", pkg.name());
                rmMeta.put("sourceFile", rm.sourceFile());
                RkgNode recNode = new RkgNode(rm.qualifiedName(), RkgNodeType.RECORD, rm.sourceFile(), rmMeta, 1);
                graph.addNode(recNode);
                graph.addEdge(new RkgEdge(pkgNode.id(), recNode.id(), RkgEdgeType.CONTAINS, 1.0, Collections.emptyList(), null));
            }
        }

        return graph;
    }

    private static RkgNode buildClassNode(ClassModel cm, String pkgName) {
        Map<String, Object> meta = new HashMap<>();
        meta.put("package", pkgName);
        meta.put("sourceFile", cm.sourceFile());
        meta.put("modifiers", cm.modifiers());
        
        List<String> annotations = cm.annotations().stream().map(a -> a.name()).toList();
        meta.put("annotations", annotations);

        return new RkgNode(
            cm.qualifiedName(),
            RkgNodeType.CLASS,
            cm.sourceFile(),
            meta,
            1
        );
    }

    private static RkgNode buildMethodNode(MethodModel mm, String classFqn, String pkgName) {
        Map<String, Object> meta = new HashMap<>();
        meta.put("package", pkgName);
        meta.put("modifiers", mm.modifiers());
        
        List<String> annotations = mm.annotations().stream().map(a -> a.name()).toList();
        meta.put("annotations", annotations);

        String id = classFqn + "::" + mm.name();

        return new RkgNode(
            id,
            RkgNodeType.METHOD,
            "line:" + mm.startLine(),
            meta,
            1
        );
    }
}
