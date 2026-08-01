package com.repodna.discovery.detectors;

import com.repodna.discovery.AnalysisContext;
import com.repodna.discovery.PatternDetector;
import com.repodna.discovery.model.DiscoveredPattern;
import com.repodna.discovery.model.PatternEvidence;
import com.repodna.graph.LayerDetector.Layer;
import com.repodna.parser.model.AnnotationDecl;
import com.repodna.parser.model.ClassDecl;
import com.repodna.parser.model.MethodDecl;
import com.repodna.parser.model.ParsedFile;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Detects transaction management patterns in the application.
 */
public class TransactionPatternDetector implements PatternDetector {
    @Override
    public String name() {
        return "Transaction Management Detector";
    }

    @Override
    public List<DiscoveredPattern> detect(AnalysisContext context) {
        List<DiscoveredPattern> patterns = new ArrayList<>();
        
        List<PatternEvidence> serviceLevelEvidence = new ArrayList<>();
        List<PatternEvidence> classLevelEvidence = new ArrayList<>();
        List<PatternEvidence> readOnlyEvidence = new ArrayList<>();
        
        int serviceMethodCount = 0;
        int txServiceMethodCount = 0;
        int txClassCount = 0;
        int txMethodCount = 0;
        int readOnlyCount = 0;
        int totalTxCount = 0;

        for (ClassDecl clazz : context.allClasses()) {
            Path filePath = getFilePath(context, clazz);
            boolean isService = context.getClassesInLayer(Layer.SERVICE).contains(clazz.name());
            boolean hasClassLevelTx = false;
            
            for (AnnotationDecl ann : clazz.annotations()) {
                if (ann.name().endsWith("Transactional")) {
                    hasClassLevelTx = true;
                    txClassCount++;
                    totalTxCount++;
                    classLevelEvidence.add(new PatternEvidence(filePath, clazz.startLine(), 
                            "@Transactional", "Class-level transaction management"));
                    
                    if (ann.attributes() != null) {
                        String readOnlyVal = ann.attributes().get("readOnly");
                        if (readOnlyVal != null && readOnlyVal.contains("true")) {
                            readOnlyCount++;
                            readOnlyEvidence.add(new PatternEvidence(filePath, clazz.startLine(), 
                                    ann.name() + "(" + ann.attributes() + ")", "Read-only class transaction"));
                        }
                    }
                }
            }

            for (MethodDecl method : clazz.methods()) {
                if (isService) {
                    serviceMethodCount++;
                }
                
                for (AnnotationDecl ann : method.annotations()) {
                    if (ann.name().endsWith("Transactional")) {
                        txMethodCount++;
                        totalTxCount++;
                        
                        if (isService) {
                            txServiceMethodCount++;
                            serviceLevelEvidence.add(new PatternEvidence(filePath, method.startLine(), 
                                    "@Transactional on " + method.name(), "Service-level transaction boundary"));
                        }
                        
                        if (ann.attributes() != null) {
                            String readOnlyVal = ann.attributes().get("readOnly");
                            if (readOnlyVal != null && readOnlyVal.contains("true")) {
                                readOnlyCount++;
                                readOnlyEvidence.add(new PatternEvidence(filePath, method.startLine(), 
                                        ann.name() + "(" + ann.attributes() + ")", "Read-only method transaction"));
                            }
                        }
                    }
                }
            }
        }

        if (txServiceMethodCount > 0) {
            patterns.add(DiscoveredPattern.of(
                    "transaction.service-level",
                    "Transaction Management",
                    "Transactions are managed at the service layer.",
                    txServiceMethodCount,
                    serviceMethodCount > 0 ? serviceMethodCount : txServiceMethodCount,
                    serviceLevelEvidence,
                    txServiceMethodCount + " service methods are transactional out of " + serviceMethodCount + " total service methods."
            ));
        }

        if (txClassCount > 0) {
            patterns.add(DiscoveredPattern.of(
                    "transaction.class-level",
                    "Transaction Management",
                    "Transactions are managed at the class level.",
                    txClassCount,
                    context.allClasses().size(),
                    classLevelEvidence,
                    "Found " + txClassCount + " classes using @Transactional."
            ));
        }

        if (readOnlyCount > 0) {
            patterns.add(DiscoveredPattern.of(
                    "transaction.read-only",
                    "Transaction Management",
                    "Usage of read-only transactions.",
                    readOnlyCount,
                    totalTxCount,
                    readOnlyEvidence,
                    readOnlyCount + " out of " + totalTxCount + " transactions are marked as read-only."
            ));
        }

        return patterns;
    }

    private Path getFilePath(AnalysisContext context, ClassDecl clazz) {
        if (context.getParsedFiles() != null) {
            for (ParsedFile pf : context.getParsedFiles()) {
                if (pf.classes() != null && pf.classes().contains(clazz)) {
                    return pf.filePath();
                }
            }
        }
        return Path.of("unknown");
    }
}
