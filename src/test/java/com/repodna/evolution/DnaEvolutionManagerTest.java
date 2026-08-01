package com.repodna.evolution;

import com.repodna.discovery.model.DiscoveredPattern;
import com.repodna.evolution.model.EvolutionScore;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class DnaEvolutionManagerTest {

    @Test
    public void testCalculateDriftEmpty() {
        DnaEvolutionManager manager = new DnaEvolutionManager();
        EvolutionScore score = manager.calculateDrift(List.of(), List.of());
        assertEquals(0.0, score.geneticDistance());
        assertFalse(score.isDrifted());
        assertTrue(score.intentionalMigrations().isEmpty());
        assertTrue(score.accidentalDrifts().isEmpty());
    }

    @Test
    public void testCalculateDriftWithConventions() {
        DnaEvolutionManager manager = new DnaEvolutionManager();

        // Baseline: Naming suffix exists
        DiscoveredPattern pBase = DiscoveredPattern.of(
            "naming.service-suffix", "Naming", "Service suffix", 5, 5, List.of(), "Strict service naming"
        );

        // Current: Suffix dropped in some files (confidence goes from 1.0 -> 0.5)
        // Also introduce a new accidental pattern
        DiscoveredPattern pCur1 = DiscoveredPattern.of(
            "naming.service-suffix", "Naming", "Service suffix", 2, 4, List.of(), "Mixed suffix"
        );
        DiscoveredPattern pCur2 = DiscoveredPattern.of(
            "security.unsafe-cors", "Security", "Unsafe CORS", 1, 1, List.of(), "Wildcard CORS"
        );

        EvolutionScore score = manager.calculateDrift(List.of(pBase), List.of(pCur1, pCur2));
        
        // Assert distance is computed
        assertTrue(score.geneticDistance() > 0.0);
        
        // Assert CORS is flagged as accidental drift
        assertFalse(score.accidentalDrifts().isEmpty());
        assertTrue(score.accidentalDrifts().get(0).contains("security.unsafe-cors"));
    }

    @Test
    public void testCalculateDriftWithMigration() {
        DnaEvolutionManager manager = new DnaEvolutionManager();

        // Baseline pattern
        DiscoveredPattern pBase = DiscoveredPattern.of(
            "dependency-boundary.field-injection", "Architecture", "Field injection", 5, 5, List.of(), "Field injection"
        );

        // Current introduces constructor injection (a known transition)
        DiscoveredPattern pCur1 = DiscoveredPattern.of(
            "dependency-boundary.field-injection", "Architecture", "Field injection", 5, 5, List.of(), "Field injection"
        );
        DiscoveredPattern pCur2 = DiscoveredPattern.of(
            "dependency-boundary.constructor-injection", "Architecture", "Constructor injection", 2, 2, List.of(), "Constructor injection"
        );

        EvolutionScore score = manager.calculateDrift(List.of(pBase), List.of(pCur1, pCur2));
        
        // Assert constructor injection is flagged as intentional migration
        assertFalse(score.intentionalMigrations().isEmpty());
        assertTrue(score.intentionalMigrations().get(0).contains("constructor-injection"));
        assertTrue(score.accidentalDrifts().isEmpty());
    }
}
