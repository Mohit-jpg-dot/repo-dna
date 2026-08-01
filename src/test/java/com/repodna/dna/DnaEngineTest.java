package com.repodna.dna;

import com.repodna.discovery.model.DiscoveredPattern;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class DnaEngineTest {

    @Test
    public void testBuildProfileEmpty() {
        DnaEngine engine = new DnaEngine();
        DnaProfile profile = engine.buildProfile("TestProject", List.of());
        assertEquals("TestProject", profile.projectName());
        assertEquals(0.0, profile.overallConfidence());
        assertTrue(profile.teamSignatures().isEmpty());
    }

    @Test
    public void testBuildProfileWithPatterns() {
        DnaEngine engine = new DnaEngine();

        // 100% occurrences/total opportunities = 1.0 confidence -> team signature
        DiscoveredPattern p1 = DiscoveredPattern.of(
            "naming.service-suffix", "Naming", "Service suffix convention", 5, 5, List.of(), "Strict service suffix naming followed"
        );
        
        // 50% occurrences = 0.5 confidence -> not a team signature
        DiscoveredPattern p2 = DiscoveredPattern.of(
            "rest.pagination", "Architecture", "Pagination convention", 2, 4, List.of(), "Mixed pagination usage"
        );

        DnaProfile profile = engine.buildProfile("MyProject", List.of(p1, p2));
        assertEquals("MyProject", profile.projectName());
        assertEquals(2, profile.totalPatterns());
        
        // Overall confidence should be average: (1.0 + 0.5) / 2 = 0.75
        assertEquals(0.75, profile.overallConfidence());

        // Assert p1 promoted to signature
        assertFalse(profile.teamSignatures().isEmpty());
        assertTrue(profile.teamSignatures().get(0).contains("Service suffix convention"));
    }
}
