package com.repodna.discovery.detectors;

import com.repodna.discovery.AnalysisContext;
import com.repodna.discovery.PatternDetector;
import com.repodna.discovery.model.DiscoveredPattern;
import com.repodna.discovery.model.PatternEvidence;
import com.repodna.parser.model.ClassDecl;
import com.repodna.parser.model.ParsedFile;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class DtoStrategyDetector implements PatternDetector {

    @Override
    public String name() {
        return "DTO Strategy Detector";
    }

    @Override
    public List<DiscoveredPattern> detect(AnalysisContext context) {
        List<DiscoveredPattern> patterns = new ArrayList<>();
        List<ClassDecl> allClasses = context.allClasses();
        if (allClasses == null || allClasses.isEmpty()) {
            return patterns;
        }

        List<PatternEvidence> namingEvidence = new ArrayList<>();
        List<PatternEvidence> recordEvidence = new ArrayList<>();
        List<PatternEvidence> packageEvidence = new ArrayList<>();
        List<PatternEvidence> mapperEvidence = new ArrayList<>();

        int dtoCount = 0;
        int mapperCount = 0;

        for (ClassDecl clazz : allClasses) {
            String name = clazz.name();
            ParsedFile parsedFile = getParsedFile(context, clazz);
            Path filePath = parsedFile != null ? parsedFile.filePath() : Path.of("unknown");

            if (name.endsWith("Dto") || name.endsWith("DTO") || name.endsWith("Request") || name.endsWith("Response")) {
                dtoCount++;
                namingEvidence.add(new PatternEvidence(filePath, clazz.startLine(), "class " + name, "Matches DTO naming convention"));

                if (clazz.classType() == ClassDecl.ClassType.RECORD) {
                    recordEvidence.add(new PatternEvidence(filePath, clazz.startLine(), "record " + name, "DTO is a record"));
                }

                String pkg = parsedFile != null ? parsedFile.packageName() : null;
                if (pkg != null && (pkg.contains(".dto") || pkg.endsWith(".dto") || pkg.contains(".request") || pkg.contains(".response"))) {
                    packageEvidence.add(new PatternEvidence(filePath, clazz.startLine(), "package " + pkg, "DTO in dedicated package"));
                }
            }

            if (name.endsWith("Mapper") || name.endsWith("Converter") || AnalysisContext.hasAnnotation(clazz, "Mapper")) {
                mapperCount++;
                mapperEvidence.add(new PatternEvidence(filePath, clazz.startLine(), "class " + name, "Mapper/Converter class found"));
            }
        }

        if (dtoCount > 0) {
            patterns.add(DiscoveredPattern.of(
                    "dto.naming-convention",
                    "Architecture",
                    "Classes following DTO naming conventions (Dto, Request, Response).",
                    dtoCount,
                    dtoCount,
                    namingEvidence,
                    "Consistent DTO naming makes data transfer objects easily identifiable."
            ));

            patterns.add(DiscoveredPattern.of(
                    "dto.uses-records",
                    "Modern Java",
                    "Using Java Records for DTOs.",
                    recordEvidence.size(),
                    dtoCount,
                    recordEvidence,
                    "Records are ideal for immutable data transfer objects."
            ));

            patterns.add(DiscoveredPattern.of(
                    "dto.dedicated-package",
                    "Architecture",
                    "DTOs organized in dedicated packages.",
                    packageEvidence.size(),
                    dtoCount,
                    packageEvidence,
                    "Organizing DTOs in a specific package helps enforce architectural boundaries."
            ));
        }

        if (mapperCount > 0) {
            patterns.add(DiscoveredPattern.of(
                    "dto.mapper-pattern",
                    "Architecture",
                    "Usage of Mapper/Converter pattern for DTOs.",
                    mapperCount,
                    mapperCount,
                    mapperEvidence,
                    "Mappers isolate conversion logic between entities and DTOs."
            ));
        }

        return patterns;
    }

    private ParsedFile getParsedFile(AnalysisContext context, ClassDecl clazz) {
        if (context.getParsedFiles() != null) {
            for (ParsedFile pf : context.getParsedFiles()) {
                if (pf.classes() != null && pf.classes().contains(clazz)) {
                    return pf;
                }
            }
        }
        return null;
    }
}
