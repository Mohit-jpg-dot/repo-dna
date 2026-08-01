package com.repodna.model;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

public record ProjectInfo(
    String name,
    Path rootPath,
    String language,
    String framework,
    String buildTool,
    boolean multiModule,
    List<String> modules,
    String javaVersion,
    Instant analyzedAt
) {}
