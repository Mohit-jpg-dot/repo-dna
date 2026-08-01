package com.repodna.health;

import com.repodna.discovery.AnalysisContext;
import com.repodna.discovery.model.DiscoveredPattern;
import com.repodna.health.model.HealthScore;
import java.util.List;

public interface HealthEvaluator {
    String dimension();
    HealthScore evaluate(AnalysisContext context, List<DiscoveredPattern> patterns);
}
