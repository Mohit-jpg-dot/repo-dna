package com.repodna.parser;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.repodna.parser.model.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Parsing engine orchestrating JavaParser syntax parsing and reference resolution.
 */
public class ParserEngine {
    private final ParserConfiguration config;
    private final List<String> unresolvedSymbols = new CopyOnWriteArrayList<>();

    public ParserEngine(List<Path> srcRoots) {
        CombinedTypeSolver typeSolver = new CombinedTypeSolver();
        typeSolver.add(new ReflectionTypeSolver());
        
        ParserConfiguration config = new ParserConfiguration()
            .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21);

        for (Path root : srcRoots) {
            typeSolver.add(new JavaParserTypeSolver(root.toFile(), config));
        }

        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(typeSolver);
        config.setSymbolResolver(symbolSolver);
        this.config = config;
    }

    /**
     * Parses a list of Java files in parallel and returns a populated ProjectModel.
     */
    public ProjectModel parse(String projectName, List<Path> files) {
        Map<String, List<ClassModel>> packageClasses = new ConcurrentHashMap<>();
        Map<String, List<InterfaceModel>> packageInterfaces = new ConcurrentHashMap<>();
        Map<String, List<EnumModel>> packageEnums = new ConcurrentHashMap<>();
        Map<String, List<RecordModel>> packageRecords = new ConcurrentHashMap<>();

        files.parallelStream().forEach(file -> {
            try {
                JavaParser parser = new JavaParser(config);
                com.github.javaparser.ParseResult<CompilationUnit> result = parser.parse(file);
                if (result.isSuccessful() && result.getResult().isPresent()) {
                    CompilationUnit cu = result.getResult().get();
                    String pkgName = cu.getPackageDeclaration()
                        .map(pd -> pd.getName().asString())
                        .orElse("default");

                    String srcFile = file.getFileName().toString();

                    for (TypeDeclaration<?> type : cu.getTypes()) {
                        if (type.isClassOrInterfaceDeclaration()) {
                            ClassOrInterfaceDeclaration cid = type.asClassOrInterfaceDeclaration();
                            if (cid.isInterface()) {
                                InterfaceModel im = buildInterfaceModel(cid, srcFile);
                                packageInterfaces.computeIfAbsent(pkgName, k -> new CopyOnWriteArrayList<>()).add(im);
                            } else {
                                ClassModel cm = buildClassModel(cid, srcFile);
                                packageClasses.computeIfAbsent(pkgName, k -> new CopyOnWriteArrayList<>()).add(cm);
                            }
                        } else if (type.isEnumDeclaration()) {
                            EnumModel em = buildEnumModel(type.asEnumDeclaration(), srcFile);
                            packageEnums.computeIfAbsent(pkgName, k -> new CopyOnWriteArrayList<>()).add(em);
                        } else if (type.isRecordDeclaration()) {
                            RecordModel rm = buildRecordModel(type.asRecordDeclaration(), srcFile);
                            packageRecords.computeIfAbsent(pkgName, k -> new CopyOnWriteArrayList<>()).add(rm);
                        }
                    }
                } else {
                    System.err.println("Parse failed for file " + file + ": " + result.getProblems());
                }
            } catch (Exception e) {
                System.err.println("Exception parsing file " + file + ": " + e.getMessage());
                e.printStackTrace();
            }
        });

        // Assemble package models
        Set<String> allPackages = new HashSet<>();
        allPackages.addAll(packageClasses.keySet());
        allPackages.addAll(packageInterfaces.keySet());
        allPackages.addAll(packageEnums.keySet());
        allPackages.addAll(packageRecords.keySet());

        List<PackageModel> packages = new ArrayList<>();
        for (String pkg : allPackages) {
            packages.add(new PackageModel(
                pkg,
                packageClasses.getOrDefault(pkg, Collections.emptyList()),
                packageInterfaces.getOrDefault(pkg, Collections.emptyList()),
                packageEnums.getOrDefault(pkg, Collections.emptyList()),
                packageRecords.getOrDefault(pkg, Collections.emptyList())
            ));
        }

        return new ProjectModel(projectName, packages, new ArrayList<>(unresolvedSymbols));
    }

    private ClassModel buildClassModel(ClassOrInterfaceDeclaration node, String srcFile) {
        String name = node.getNameAsString();
        String qualifiedName = name;
        try {
            qualifiedName = node.resolve().getQualifiedName();
        } catch (Exception e) {
            unresolvedSymbols.add(name);
        }

        Set<String> modifiers = node.getModifiers().stream()
            .map(m -> m.getKeyword().asString())
            .collect(Collectors.toSet());

        TypeReference superClass = node.getExtendedTypes().isEmpty() ? null 
            : resolveType(node.getExtendedTypes(0));

        List<TypeReference> interfaces = node.getImplementedTypes().stream()
            .map(this::resolveType)
            .toList();

        List<AnnotationModel> annotations = node.getAnnotations().stream()
            .map(this::resolveAnnotation)
            .toList();

        List<ConstructorModel> constructors = node.getConstructors().stream()
            .map(c -> buildConstructorModel(c, name))
            .toList();

        List<MethodModel> methods = node.getMethods().stream()
            .map(this::buildMethodModel)
            .toList();

        List<FieldModel> fields = node.getFields().stream()
            .flatMap(f -> buildFieldModels(f).stream())
            .toList();

        List<String> typeParameters = node.getTypeParameters().stream()
            .map(tp -> tp.getNameAsString())
            .toList();

        List<ClassModel> nestedClasses = node.getMembers().stream()
            .filter(m -> m.isClassOrInterfaceDeclaration() && !m.asClassOrInterfaceDeclaration().isInterface())
            .map(m -> buildClassModel(m.asClassOrInterfaceDeclaration(), srcFile))
            .toList();

        int startLine = node.getBegin().map(p -> p.line).orElse(0);
        int endLine = node.getEnd().map(p -> p.line).orElse(0);

        return new ClassModel(
            name, qualifiedName, modifiers, superClass, interfaces, annotations,
            constructors, methods, fields, typeParameters, nestedClasses, startLine, endLine, srcFile
        );
    }

    private InterfaceModel buildInterfaceModel(ClassOrInterfaceDeclaration node, String srcFile) {
        String name = node.getNameAsString();
        String qualifiedName = name;
        try {
            qualifiedName = node.resolve().getQualifiedName();
        } catch (Exception e) {
            unresolvedSymbols.add(name);
        }

        Set<String> modifiers = node.getModifiers().stream()
            .map(m -> m.getKeyword().asString())
            .collect(Collectors.toSet());

        List<TypeReference> interfaces = node.getExtendedTypes().stream()
            .map(this::resolveType)
            .toList();

        List<AnnotationModel> annotations = node.getAnnotations().stream()
            .map(this::resolveAnnotation)
            .toList();

        List<MethodModel> methods = node.getMethods().stream()
            .map(this::buildMethodModel)
            .toList();

        int startLine = node.getBegin().map(p -> p.line).orElse(0);
        int endLine = node.getEnd().map(p -> p.line).orElse(0);

        return new InterfaceModel(
            name, qualifiedName, modifiers, interfaces, annotations, methods, startLine, endLine, srcFile
        );
    }

    private EnumModel buildEnumModel(EnumDeclaration node, String srcFile) {
        String name = node.getNameAsString();
        String qualifiedName = name;
        try {
            qualifiedName = node.resolve().getQualifiedName();
        } catch (Exception e) {
            unresolvedSymbols.add(name);
        }

        Set<String> modifiers = node.getModifiers().stream()
            .map(m -> m.getKeyword().asString())
            .collect(Collectors.toSet());

        List<AnnotationModel> annotations = node.getAnnotations().stream()
            .map(this::resolveAnnotation)
            .toList();

        List<String> entries = node.getEntries().stream()
            .map(ee -> ee.getNameAsString())
            .toList();

        List<ConstructorModel> constructors = node.getConstructors().stream()
            .map(c -> buildConstructorModel(c, name))
            .toList();

        List<MethodModel> methods = node.getMethods().stream()
            .map(this::buildMethodModel)
            .toList();

        List<FieldModel> fields = node.getFields().stream()
            .flatMap(f -> buildFieldModels(f).stream())
            .toList();

        int startLine = node.getBegin().map(p -> p.line).orElse(0);
        int endLine = node.getEnd().map(p -> p.line).orElse(0);

        return new EnumModel(
            name, qualifiedName, modifiers, annotations, entries, constructors, methods, fields, startLine, endLine, srcFile
        );
    }

    private RecordModel buildRecordModel(RecordDeclaration node, String srcFile) {
        String name = node.getNameAsString();
        String qualifiedName = name;
        try {
            qualifiedName = node.resolve().getQualifiedName();
        } catch (Exception e) {
            unresolvedSymbols.add(name);
        }

        Set<String> modifiers = node.getModifiers().stream()
            .map(m -> m.getKeyword().asString())
            .collect(Collectors.toSet());

        List<AnnotationModel> annotations = node.getAnnotations().stream()
            .map(this::resolveAnnotation)
            .toList();

        List<ParameterModel> components = node.getParameters().stream()
            .map(p -> new ParameterModel(
                p.getNameAsString(),
                resolveType(p.getType()),
                p.getAnnotations().stream().map(this::resolveAnnotation).toList()
            ))
            .toList();

        List<TypeReference> interfaces = node.getImplementedTypes().stream()
            .map(this::resolveType)
            .toList();

        List<ConstructorModel> constructors = node.getConstructors().stream()
            .map(c -> buildConstructorModel(c, name))
            .toList();

        List<MethodModel> methods = node.getMethods().stream()
            .map(this::buildMethodModel)
            .toList();

        int startLine = node.getBegin().map(p -> p.line).orElse(0);
        int endLine = node.getEnd().map(p -> p.line).orElse(0);

        return new RecordModel(
            name, qualifiedName, modifiers, annotations, components, interfaces, constructors, methods, startLine, endLine, srcFile
        );
    }

    private ConstructorModel buildConstructorModel(ConstructorDeclaration node, String className) {
        String name = node.getNameAsString();
        String id = className + "::" + name;

        List<ParameterModel> parameters = node.getParameters().stream()
            .map(p -> new ParameterModel(
                p.getNameAsString(),
                resolveType(p.getType()),
                p.getAnnotations().stream().map(this::resolveAnnotation).toList()
            ))
            .toList();

        Set<String> modifiers = node.getModifiers().stream()
            .map(m -> m.getKeyword().asString())
            .collect(Collectors.toSet());

        List<AnnotationModel> annotations = node.getAnnotations().stream()
            .map(this::resolveAnnotation)
            .toList();

        List<CallSiteModel> calls = extractCallSites(node);

        int startLine = node.getBegin().map(p -> p.line).orElse(0);
        int endLine = node.getEnd().map(p -> p.line).orElse(0);

        return new ConstructorModel(id, name, parameters, modifiers, annotations, startLine, endLine, calls);
    }

    private MethodModel buildMethodModel(MethodDeclaration node) {
        String name = node.getNameAsString();
        String id = name;

        List<ParameterModel> parameters = node.getParameters().stream()
            .map(p -> new ParameterModel(
                p.getNameAsString(),
                resolveType(p.getType()),
                p.getAnnotations().stream().map(this::resolveAnnotation).toList()
            ))
            .toList();

        Set<String> modifiers = node.getModifiers().stream()
            .map(m -> m.getKeyword().asString())
            .collect(Collectors.toSet());

        List<AnnotationModel> annotations = node.getAnnotations().stream()
            .map(this::resolveAnnotation)
            .toList();

        TypeReference returnType = resolveType(node.getType());
        List<CallSiteModel> calls = extractCallSites(node);

        int startLine = node.getBegin().map(p -> p.line).orElse(0);
        int endLine = node.getEnd().map(p -> p.line).orElse(0);

        return new MethodModel(
            id, name, returnType, parameters, modifiers, annotations, startLine, endLine, calls, node.isAbstract()
        );
    }

    private List<FieldModel> buildFieldModels(FieldDeclaration node) {
        List<FieldModel> fields = new ArrayList<>();
        TypeReference typeRef = resolveType(node.getElementType());
        Set<String> modifiers = node.getModifiers().stream()
            .map(m -> m.getKeyword().asString())
            .collect(Collectors.toSet());

        List<AnnotationModel> annotations = node.getAnnotations().stream()
            .map(this::resolveAnnotation)
            .toList();

        int line = node.getBegin().map(p -> p.line).orElse(0);

        for (VariableDeclarator var : node.getVariables()) {
            fields.add(new FieldModel(
                var.getNameAsString(),
                typeRef,
                modifiers,
                annotations,
                var.getInitializer().isPresent(),
                line
            ));
        }
        return fields;
    }

    private TypeReference resolveType(Type type) {
        String name = type.asString();
        try {
            String qName = type.resolve().describe();
            return new TypeReference(name, qName, true);
        } catch (Exception e) {
            unresolvedSymbols.add(name);
            return new TypeReference(name, name, false);
        }
    }

    private AnnotationModel resolveAnnotation(AnnotationExpr node) {
        String name = node.getNameAsString();
        String qName = name;
        try {
            qName = node.resolve().getQualifiedName();
        } catch (Exception e) {
            unresolvedSymbols.add(name);
        }

        Map<String, String> values = new HashMap<>();
        if (node.isNormalAnnotationExpr()) {
            for (MemberValuePair pair : node.asNormalAnnotationExpr().getPairs()) {
                values.put(pair.getNameAsString(), pair.getValue().toString());
            }
        } else if (node.isSingleMemberAnnotationExpr()) {
            values.put("value", node.asSingleMemberAnnotationExpr().getMemberValue().toString());
        }

        return new AnnotationModel(name, qName, values);
    }

    private List<CallSiteModel> extractCallSites(Node node) {
        List<CallSiteModel> calls = new ArrayList<>();
        node.findAll(MethodCallExpr.class).forEach(call -> {
            String calleeName = "unknown";
            String calleeType = "unknown";
            try {
                var resolved = call.resolve();
                calleeName = resolved.getName();
                calleeType = resolved.declaringType().getQualifiedName();
            } catch (Exception e) {
                unresolvedSymbols.add(call.getNameAsString());
            }
            int line = call.getBegin().map(p -> p.line).orElse(0);
            calls.add(new CallSiteModel(calleeName, calleeType, call.getNameAsString(), line));
        });
        return calls;
    }
}
