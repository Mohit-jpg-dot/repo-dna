package com.repodna.parser;

import com.repodna.parser.model.AnnotationModel;
import com.repodna.parser.model.ClassModel;
import com.repodna.parser.model.ConstructorModel;
import com.repodna.parser.model.FieldModel;
import com.repodna.parser.model.MethodModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Deterministically analyzes Java ClassModels to detect Spring Boot stereotypes and dependency injection styles.
 */
public class SpringDetector {
    private static final Set<String> STEREOTYPES = Set.of(
        "Controller", "RestController", "Service", "Repository", "Component", "Configuration", "Entity"
    );

    public record SpringMeta(
        boolean isSpringManaged,
        List<String> stereotypes,
        String injectionStyle, // "Constructor Injection", "Field Injection", "None"
        List<String> endpoints
    ) {}

    /**
     * Inspects ClassModel metadata for Spring framework hints.
     */
    public static SpringMeta detect(ClassModel clazz) {
        List<String> stereotypes = new ArrayList<>();
        List<String> endpoints = new ArrayList<>();

        for (AnnotationModel anno : clazz.annotations()) {
            String name = anno.name();
            if (STEREOTYPES.contains(name)) {
                stereotypes.add(name);
            }
            if ("RequestMapping".equals(name)) {
                String path = anno.values().getOrDefault("value", "/");
                endpoints.add("Class Root: " + path);
            }
        }

        for (MethodModel method : clazz.methods()) {
            for (AnnotationModel anno : method.annotations()) {
                String name = anno.name();
                if ("Bean".equals(name)) {
                    stereotypes.add("Bean Method: " + method.name());
                }
                if (name.endsWith("Mapping")) {
                    String path = anno.values().getOrDefault("value", "/");
                    endpoints.add(name + " on " + method.name() + ": " + path);
                }
            }
        }

        // Determine injection style
        boolean hasFieldInjection = false;
        for (FieldModel field : clazz.fields()) {
            boolean isAutowired = field.annotations().stream()
                .anyMatch(a -> "Autowired".equals(a.name()) || "Value".equals(a.name()));
            if (isAutowired) {
                hasFieldInjection = true;
                break;
            }
        }

        boolean hasConstructorInjection = false;
        boolean hasAutowiredConstructor = false;
        
        for (ConstructorModel cons : clazz.constructors()) {
            boolean isAutowired = cons.annotations().stream()
                .anyMatch(a -> "Autowired".equals(a.name()));
            if (isAutowired) {
                hasAutowiredConstructor = true;
            }
            if (!cons.parameters().isEmpty()) {
                hasConstructorInjection = true;
            }
        }

        String injectionStyle = "None";
        if (hasFieldInjection) {
            injectionStyle = "Field Injection";
        } else if (hasAutowiredConstructor || (hasConstructorInjection && !clazz.constructors().isEmpty() && !hasFieldInjection)) {
            injectionStyle = "Constructor Injection";
        }

        boolean isManaged = !stereotypes.isEmpty() 
            || !clazz.constructors().stream().filter(c -> c.annotations().stream().anyMatch(a -> "Autowired".equals(a.name()))).toList().isEmpty();

        return new SpringMeta(isManaged, stereotypes, injectionStyle, endpoints);
    }
}
