package com.repodna.parser;

import com.repodna.parser.model.AnnotationDecl;
import com.repodna.parser.model.ClassDecl;
import com.repodna.parser.model.FieldDecl;
import com.repodna.parser.model.ImportDecl;
import com.repodna.parser.model.MethodDecl;
import com.repodna.parser.model.ParsedFile;
import org.treesitter.TSNode;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Walker to extract AST nodes into the RepoDNA data model.
 */
public class AstWalker {

    public static ParsedFile walk(Path filePath, TSNode root, String source) {
        String packageName = null;
        List<ImportDecl> imports = new ArrayList<>();
        List<ClassDecl> classes = new ArrayList<>();

        int childCount = root.getNamedChildCount();
        for (int i = 0; i < childCount; i++) {
            TSNode child = root.getNamedChild(i);
            String type = child.getType();

            if ("package_declaration".equals(type)) {
                TSNode nameNode = child.getChildByFieldName("name");
                if (nameNode != null) {
                    packageName = getNodeText(nameNode, source);
                }
            } else if ("import_declaration".equals(type)) {
                imports.add(extractImport(child, source));
            } else if (isClassDeclaration(type)) {
                classes.add(extractClass(child, source, type));
            }
        }

        return new ParsedFile(filePath, packageName, imports, classes);
    }

    private static boolean isClassDeclaration(String type) {
        return "class_declaration".equals(type) ||
               "interface_declaration".equals(type) ||
               "enum_declaration".equals(type) ||
               "record_declaration".equals(type) ||
               "annotation_type_declaration".equals(type);
    }

    private static ClassDecl.ClassType mapClassType(String type) {
        return switch (type) {
            case "interface_declaration" -> ClassDecl.ClassType.INTERFACE;
            case "enum_declaration" -> ClassDecl.ClassType.ENUM;
            case "record_declaration" -> ClassDecl.ClassType.RECORD;
            case "annotation_type_declaration" -> ClassDecl.ClassType.ANNOTATION;
            default -> ClassDecl.ClassType.CLASS;
        };
    }

    private static ImportDecl extractImport(TSNode node, String source) {
        boolean isStatic = false;
        boolean isWildcard = false;
        String name = "";

        int count = node.getChildCount();
        for (int i = 0; i < count; i++) {
            TSNode child = node.getChild(i);
            String type = child.getType();
            if ("static".equals(type)) {
                isStatic = true;
            } else if ("asterisk".equals(type)) {
                isWildcard = true;
            } else if ("scoped_identifier".equals(type) || "identifier".equals(type)) {
                name = getNodeText(child, source);
            }
        }
        return new ImportDecl(name, isStatic, isWildcard);
    }

    private static ClassDecl extractClass(TSNode node, String source, String nodeType) {
        TSNode nameNode = node.getChildByFieldName("name");
        String name = nameNode != null ? getNodeText(nameNode, source) : "Anonymous";

        Set<String> modifiers = new HashSet<>();
        List<AnnotationDecl> annotations = new ArrayList<>();
        
        extractModifiersAndAnnotations(node, source, modifiers, annotations);

        String superClass = null;
        TSNode superclassNode = node.getChildByFieldName("superclass");
        if (superclassNode != null) {
            superClass = getNodeText(superclassNode, source);
        }

        List<String> interfaces = new ArrayList<>();
        TSNode interfacesNode = node.getChildByFieldName("interfaces");
        if (interfacesNode != null) {
            int childCount = interfacesNode.getNamedChildCount();
            for (int i = 0; i < childCount; i++) {
                interfaces.add(getNodeText(interfacesNode.getNamedChild(i), source));
            }
        }

        List<MethodDecl> methods = new ArrayList<>();
        List<FieldDecl> fields = new ArrayList<>();
        List<ClassDecl> innerClasses = new ArrayList<>();

        TSNode bodyNode = node.getChildByFieldName("body");
        if (bodyNode != null) {
            int bodyChildCount = bodyNode.getNamedChildCount();
            for (int i = 0; i < bodyChildCount; i++) {
                TSNode bodyChild = bodyNode.getNamedChild(i);
                String childType = bodyChild.getType();

                if ("method_declaration".equals(childType)) {
                    methods.add(extractMethod(bodyChild, source, false));
                } else if ("constructor_declaration".equals(childType)) {
                    methods.add(extractMethod(bodyChild, source, true));
                } else if ("field_declaration".equals(childType)) {
                    fields.add(extractField(bodyChild, source));
                } else if (isClassDeclaration(childType)) {
                    innerClasses.add(extractClass(bodyChild, source, childType));
                }
            }
        }

        return new ClassDecl(name, mapClassType(nodeType), modifiers, superClass, interfaces, annotations, methods, fields, innerClasses, node.getStartPoint().getRow() + 1);
    }

    private static MethodDecl extractMethod(TSNode node, String source, boolean isConstructor) {
        TSNode nameNode = node.getChildByFieldName("name");
        String name = nameNode != null ? getNodeText(nameNode, source) : (isConstructor ? "constructor" : "unknown");

        String returnType = null;
        if (!isConstructor) {
            TSNode typeNode = node.getChildByFieldName("type");
            if (typeNode != null) {
                returnType = getNodeText(typeNode, source);
            }
        }

        Set<String> modifiers = new HashSet<>();
        List<AnnotationDecl> annotations = new ArrayList<>();
        extractModifiersAndAnnotations(node, source, modifiers, annotations);

        List<MethodDecl.ParameterInfo> parameters = new ArrayList<>();
        TSNode paramsNode = node.getChildByFieldName("parameters");
        if (paramsNode != null) {
            int paramCount = paramsNode.getNamedChildCount();
            for (int i = 0; i < paramCount; i++) {
                TSNode paramNode = paramsNode.getNamedChild(i);
                if ("formal_parameter".equals(paramNode.getType()) || "spread_parameter".equals(paramNode.getType())) {
                    TSNode paramNameNode = paramNode.getChildByFieldName("name");
                    TSNode paramTypeNode = paramNode.getChildByFieldName("type");
                    String paramName = paramNameNode != null ? getNodeText(paramNameNode, source) : "";
                    String paramType = paramTypeNode != null ? getNodeText(paramTypeNode, source) : "";
                    
                    List<AnnotationDecl> paramAnns = new ArrayList<>();
                    // Basic param annotation extraction if needed
                    for (int j = 0; j < paramNode.getNamedChildCount(); j++) {
                        TSNode pChild = paramNode.getNamedChild(j);
                        if ("modifiers".equals(pChild.getType())) {
                             for (int k = 0; k < pChild.getNamedChildCount(); k++) {
                                 TSNode mChild = pChild.getNamedChild(k);
                                 if (mChild.getType().contains("annotation")) {
                                     paramAnns.add(extractAnnotation(mChild, source));
                                 }
                             }
                        }
                        if (pChild.getType().contains("annotation")) {
                             paramAnns.add(extractAnnotation(pChild, source));
                        }
                    }

                    parameters.add(new MethodDecl.ParameterInfo(paramName, paramType, paramAnns));
                }
            }
        }

        int bodyLineCount = 0;
        List<String> methodCalls = new ArrayList<>();
        TSNode bodyNode = node.getChildByFieldName("body");
        if (bodyNode != null) {
            bodyLineCount = Math.max(0, bodyNode.getEndPoint().getRow() - bodyNode.getStartPoint().getRow() + 1);
            extractMethodCalls(bodyNode, source, methodCalls);
        }

        return new MethodDecl(name, returnType, parameters, modifiers, annotations, bodyLineCount, methodCalls, isConstructor, node.getStartPoint().getRow() + 1);
    }

    private static void extractMethodCalls(TSNode node, String source, List<String> calls) {
        if ("method_invocation".equals(node.getType())) {
            TSNode nameNode = node.getChildByFieldName("name");
            if (nameNode != null) {
                calls.add(getNodeText(nameNode, source));
            }
        }
        for (int i = 0; i < node.getNamedChildCount(); i++) {
            extractMethodCalls(node.getNamedChild(i), source, calls);
        }
    }

    private static FieldDecl extractField(TSNode node, String source) {
        String type = "";
        TSNode typeNode = node.getChildByFieldName("type");
        if (typeNode != null) {
            type = getNodeText(typeNode, source);
        }

        Set<String> modifiers = new HashSet<>();
        List<AnnotationDecl> annotations = new ArrayList<>();
        extractModifiersAndAnnotations(node, source, modifiers, annotations);

        String name = "";
        boolean hasInitializer = false;
        
        TSNode declNode = node.getChildByFieldName("declarator");
        if (declNode == null) {
            // fallback
            for (int i = 0; i < node.getNamedChildCount(); i++) {
                TSNode child = node.getNamedChild(i);
                if ("variable_declarator".equals(child.getType())) {
                    declNode = child;
                    break;
                }
            }
        }

        if (declNode != null) {
            TSNode nameNode = declNode.getChildByFieldName("name");
            if (nameNode != null) {
                name = getNodeText(nameNode, source);
            }
            TSNode valueNode = declNode.getChildByFieldName("value");
            if (valueNode != null) {
                hasInitializer = true;
            }
        }

        return new FieldDecl(name, type, modifiers, annotations, hasInitializer, node.getStartPoint().getRow() + 1);
    }

    private static void extractModifiersAndAnnotations(TSNode parent, String source, Set<String> modifiers, List<AnnotationDecl> annotations) {
        for (int i = 0; i < parent.getNamedChildCount(); i++) {
            TSNode child = parent.getNamedChild(i);
            if ("modifiers".equals(child.getType())) {
                for (int j = 0; j < child.getNamedChildCount(); j++) {
                    TSNode modChild = child.getNamedChild(j);
                    if ("annotation".equals(modChild.getType()) || "marker_annotation".equals(modChild.getType())) {
                        annotations.add(extractAnnotation(modChild, source));
                    } else {
                        modifiers.add(getNodeText(modChild, source));
                    }
                }
                break; // typically only one modifiers node
            }
        }
    }

    private static AnnotationDecl extractAnnotation(TSNode node, String source) {
        TSNode nameNode = node.getChildByFieldName("name");
        String name = nameNode != null ? getNodeText(nameNode, source) : "";

        Map<String, String> attributes = new HashMap<>();
        TSNode argsNode = node.getChildByFieldName("arguments");
        if (argsNode != null) {
            for (int i = 0; i < argsNode.getNamedChildCount(); i++) {
                TSNode argChild = argsNode.getNamedChild(i);
                if ("element_value_pair".equals(argChild.getType())) {
                    TSNode keyNode = argChild.getChildByFieldName("key");
                    TSNode valueNode = argChild.getChildByFieldName("value");
                    if (keyNode != null && valueNode != null) {
                        attributes.put(getNodeText(keyNode, source), getNodeText(valueNode, source));
                    }
                } else {
                    // For single-value annotations like @Service("myService"), the argument might not be a key-value pair.
                    attributes.put("value", getNodeText(argChild, source));
                }
            }
        }
        return new AnnotationDecl(name, attributes);
    }

    private static String getNodeText(TSNode node, String source) {
        if (node == null) return "";
        return source.substring(node.getStartByte(), node.getEndByte());
    }
}
