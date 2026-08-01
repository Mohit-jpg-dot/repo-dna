package com.repodna.discovery;

import com.repodna.discovery.model.DiscoveredPattern;
import java.util.List;

public interface PatternDetector {
    String name();
    List<DiscoveredPattern> detect(AnalysisContext context);
}
