package com.repodna.discovery.detectors;

import com.repodna.discovery.AnalysisContext;
import com.repodna.discovery.PatternDetector;
import com.repodna.discovery.model.DiscoveredPattern;
import com.repodna.discovery.model.PatternEvidence;
import com.repodna.parser.model.ClassDecl;
import com.repodna.parser.model.MethodDecl;
import com.repodna.parser.model.ParsedFile;
import com.repodna.parser.model.ImportDecl;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class TestingConventionDetector implements PatternDetector {

    @Override
    public String name() {
        return "Testing Convention Detector";
    }

    @Override
    public List<DiscoveredPattern> detect(AnalysisContext context) {
        List<DiscoveredPattern> patterns = new ArrayList<>();
        List<ParsedFile> parsedFiles = context.getParsedFiles();
        if (parsedFiles == null || parsedFiles.isEmpty()) {
            return patterns;
        }

        List<PatternEvidence> namingEvidence = new ArrayList<>();
        List<PatternEvidence> classNamingEvidence = new ArrayList<>();
        List<PatternEvidence> mockEvidence = new ArrayList<>();
        List<PatternEvidence> assertionEvidence = new ArrayList<>();
        List<PatternEvidence> integrationTestEvidence = new ArrayList<>();
        List<PatternEvidence> sourceRatioEvidence = new ArrayList<>();

        int testClassesCount = 0;
        int sourceClassesCount = 0;
        int mockOpportunities = 0;
        int assertOpportunities = 0;
        int testMethodCount = 0;

        for (ParsedFile pf : parsedFiles) {
            boolean isTestFile = pf.filePath().toString().contains("src/test/java") || pf.filePath().toString().endsWith("Test.java");
            
            if (!isTestFile) {
                sourceClassesCount++;
                continue;
            }

            testClassesCount++;

            // Imports check
            boolean hasMockito = false;
            boolean hasAssertJ = false;
            boolean hasJUnitAssert = false;
            
            if (pf.imports() != null) {
                for (ImportDecl imp : pf.imports()) {
                    if (imp.qualifiedName().contains("org.mockito")) hasMockito = true;
                    if (imp.qualifiedName().contains("org.assertj")) hasAssertJ = true;
                    if (imp.qualifiedName().contains("org.junit.jupiter.api.Assertions") || imp.qualifiedName().contains("org.junit.Assert")) hasJUnitAssert = true;
                }
            }

            if (hasMockito) {
                mockOpportunities++;
                mockEvidence.add(new PatternEvidence(pf.filePath(), 1, "import org.mockito.*", "Mockito framework usage"));
            }
            if (hasAssertJ || hasJUnitAssert) {
                assertOpportunities++;
                assertionEvidence.add(new PatternEvidence(pf.filePath(), 1, hasAssertJ ? "AssertJ" : "JUnit Assertions", "Assertion library usage"));
            }

            if (pf.classes() != null) {
                for (ClassDecl clazz : pf.classes()) {
                    String className = clazz.name();
                    if (className.endsWith("Test") || className.endsWith("Tests")) {
                        classNamingEvidence.add(new PatternEvidence(pf.filePath(), clazz.startLine(), className, "Standard unit test naming"));
                    } else if (className.endsWith("IT")) {
                        classNamingEvidence.add(new PatternEvidence(pf.filePath(), clazz.startLine(), className, "Integration test naming"));
                        integrationTestEvidence.add(new PatternEvidence(pf.filePath(), clazz.startLine(), className, "Integration Test"));
                    }

                    if (AnalysisContext.hasAnnotation(clazz, "SpringBootTest")) {
                        integrationTestEvidence.add(new PatternEvidence(pf.filePath(), clazz.startLine(), "@SpringBootTest", "Spring Boot integration test"));
                    }

                    if (clazz.methods() != null) {
                        for (MethodDecl method : clazz.methods()) {
                            if (AnalysisContext.hasAnnotation(method, "Test")) {
                                testMethodCount++;
                                String mName = method.name();
                                if (mName.startsWith("should") || mName.startsWith("test") || mName.contains("_")) {
                                    namingEvidence.add(new PatternEvidence(pf.filePath(), method.startLine(), mName, "Descriptive test method naming style"));
                                }
                            }
                        }
                    }
                }
            }
        }

        if (testClassesCount > 0) {
            double ratio = (double) testClassesCount / Math.max(1, sourceClassesCount);
            sourceRatioEvidence.add(new PatternEvidence(Path.of("global"), 1, String.format("Ratio: %.2f", ratio), "Test to source ratio: " + testClassesCount + " tests to " + sourceClassesCount + " sources"));
            patterns.add(DiscoveredPattern.of(
                    "testing.source-ratio",
                    "Testing",
                    "Test coverage presence based on test file count.",
                    testClassesCount,
                    testClassesCount + sourceClassesCount,
                    sourceRatioEvidence,
                    "Maintaining a healthy test-to-source ratio indicates good testing culture."
            ));
        }

        if (!namingEvidence.isEmpty()) {
            patterns.add(DiscoveredPattern.of(
                    "testing.naming-style",
                    "Testing",
                    "Test method naming conventions (should*, test*, given_when_then).",
                    namingEvidence.size(),
                    testMethodCount,
                    namingEvidence,
                    "Consistent test method names make it easier to understand behavior under test."
            ));
        }

        if (!classNamingEvidence.isEmpty()) {
            patterns.add(DiscoveredPattern.of(
                    "testing.class-naming",
                    "Testing",
                    "Test class naming conventions (*Test, *IT).",
                    classNamingEvidence.size(),
                    testClassesCount,
                    classNamingEvidence,
                    "Consistent test class naming helps build tools separate unit and integration tests."
            ));
        }

        if (mockOpportunities > 0) {
            patterns.add(DiscoveredPattern.of(
                    "testing.mock-framework",
                    "Testing",
                    "Usage of Mocking framework (Mockito).",
                    mockEvidence.size(),
                    testClassesCount,
                    mockEvidence,
                    "Mocking frameworks are essential for isolated unit testing."
            ));
        }

        if (assertOpportunities > 0) {
            patterns.add(DiscoveredPattern.of(
                    "testing.assertion-library",
                    "Testing",
                    "Usage of Assertion libraries (AssertJ, JUnit).",
                    assertionEvidence.size(),
                    testClassesCount,
                    assertionEvidence,
                    "Fluent assertion libraries improve test readability."
            ));
        }

        if (!integrationTestEvidence.isEmpty()) {
            patterns.add(DiscoveredPattern.of(
                    "testing.integration-tests",
                    "Testing",
                    "Presence of Integration tests (@SpringBootTest or *IT).",
                    integrationTestEvidence.size(),
                    testClassesCount,
                    integrationTestEvidence,
                    "Integration tests verify components working together."
            ));
        }

        return patterns;
    }
}
