package com.repodna.health;

import com.repodna.discovery.AnalysisContext;
import com.repodna.discovery.model.DiscoveredPattern;
import com.repodna.health.model.HealthReport;
import com.repodna.health.model.HealthScore;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class HealthEngineTest {

    @Test
    public void testEvaluateWithEmptyContext() {
        HealthEngine engine = new HealthEngine();
        AnalysisContext context = new AnalysisContext(
            null, List.of(), List.of(), null, null, Map.of(), List.of()
        );
        
        HealthReport report = engine.evaluate(context, List.of(), 0);
        assertNotNull(report);
        
        // Assert overall score has a reasonable default
        assertTrue(report.overallScore() >= 0 && report.overallScore() <= 100);
        assertNotNull(report.overallGrade());
        
        // Assert all 6 dimensions evaluated
        assertEquals(6, report.dimensionScores().size());
        
        // Assert dimensions present
        boolean hasArch = report.dimensionScores().stream().anyMatch(s -> s.dimension().equals("Architecture"));
        assertTrue(hasArch);
    }
}
