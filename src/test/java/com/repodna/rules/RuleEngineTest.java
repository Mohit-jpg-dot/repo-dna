package com.repodna.rules;

import com.repodna.discovery.AnalysisContext;
import com.repodna.discovery.model.PatternEvidence;
import com.repodna.parser.model.*;
import com.repodna.rules.model.EngineeringRule;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class RuleEngineTest {

    @Test
    public void testEvaluateRulesNoViolations() {
        RuleEngine engine = new RuleEngine();
        AnalysisContext context = new AnalysisContext(
            null, List.of(), List.of(), null, null, Map.of(), List.of()
        );
        
        EngineeringRule rule = new EngineeringRule(
            "naming.service-suffix", "Naming", "Service naming", "rationale", 1.0, 5, List.of(), List.of()
        );
        
        List<EngineeringRule> evaluated = engine.evaluateRules(context, List.of(rule));
        assertEquals(1, evaluated.size());
        assertTrue(evaluated.get(0).violations().isEmpty());
    }

    @Test
    public void testEvaluateNamingViolation() {
        RuleEngine engine = new RuleEngine();
        
        // MyServiceHelper class containing "Service" but doesn't end with "Service"
        ClassDecl clazz = new ClassDecl("MyServiceHelper", ClassDecl.ClassType.CLASS, Set.of("public"), null, List.of(), List.of(), List.of(), List.of(), List.of(), 1);
        
        ParsedFile pf = new ParsedFile(
            Path.of("src/main/java/com/example/MyServiceHelper.java"),
            "com.example",
            List.of(),
            List.of(clazz)
        );

        AnalysisContext context = new AnalysisContext(
            null, List.of(), List.of(pf), null, null, Map.of(), List.of()
        );
        
        EngineeringRule rule = new EngineeringRule(
            "naming.service-suffix", "Naming", "Service naming suffix", "rationale", 1.0, 5, List.of(), List.of()
        );
        
        List<EngineeringRule> evaluated = engine.evaluateRules(context, List.of(rule));
        assertEquals(1, evaluated.size());
        
        // Assert violation found
        assertFalse(evaluated.get(0).violations().isEmpty());
        assertEquals("MyServiceHelper", clazz.name());
        assertTrue(evaluated.get(0).violations().get(0).description().contains("MyServiceHelper"));
    }
}
