package com.repodna.graph;

import com.repodna.parser.model.*;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class DependencyGraphBuilderTest {

    @Test
    public void testSimpleNameCollisionResolution() {
        // 1. com.example.admin.User class
        ClassDecl adminUserClass = new ClassDecl("User", ClassDecl.ClassType.CLASS, Set.of("public"), null, List.of(), List.of(), List.of(), List.of(), List.of(), 1);
        ParsedFile adminUserFile = new ParsedFile(
            Path.of("src/main/java/com/example/admin/User.java"),
            "com.example.admin",
            List.of(),
            List.of(adminUserClass)
        );

        // 2. com.example.auth.User class (collision)
        ClassDecl authUserClass = new ClassDecl("User", ClassDecl.ClassType.CLASS, Set.of("public"), null, List.of(), List.of(), List.of(), List.of(), List.of(), 1);
        ParsedFile authUserFile = new ParsedFile(
            Path.of("src/main/java/com/example/auth/User.java"),
            "com.example.auth",
            List.of(),
            List.of(authUserClass)
        );

        // 3. com.example.service.UserService importing com.example.admin.User
        ImportDecl adminUserImport = new ImportDecl("com.example.admin.User", false, false);
        FieldDecl userField = new FieldDecl("user", "User", Set.of("private"), List.of(), false, 10);
        
        ClassDecl userServiceClass = new ClassDecl("UserService", ClassDecl.ClassType.CLASS, Set.of("public"), null, List.of(), List.of(), List.of(), List.of(userField), List.of(), 1);
        ParsedFile userServiceFile = new ParsedFile(
            Path.of("src/main/java/com/example/service/UserService.java"),
            "com.example.service",
            List.of(adminUserImport),
            List.of(userServiceClass)
        );

        List<ParsedFile> parsedFiles = List.of(adminUserFile, authUserFile, userServiceFile);
        DependencyGraphBuilder builder = new DependencyGraphBuilder(parsedFiles);
        DefaultDirectedGraph<String, DefaultEdge> graph = builder.buildClassGraph();

        // Assert vertices are present
        assertTrue(graph.containsVertex("com.example.admin.User"));
        assertTrue(graph.containsVertex("com.example.auth.User"));
        assertTrue(graph.containsVertex("com.example.service.UserService"));

        // Assert edge was correctly added to com.example.admin.User (import resolved)
        // and NOT com.example.auth.User
        assertTrue(graph.containsEdge("com.example.service.UserService", "com.example.admin.User"));
        assertFalse(graph.containsEdge("com.example.service.UserService", "com.example.auth.User"));
    }
}
