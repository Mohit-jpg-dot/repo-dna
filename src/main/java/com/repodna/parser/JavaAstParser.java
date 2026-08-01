package com.repodna.parser;

import com.repodna.parser.model.ParsedFile;
import org.treesitter.TSParser;
import org.treesitter.TSNode;
import org.treesitter.TSTree;
import org.treesitter.TreeSitterJava;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Wrapper for TSParser to parse Java code.
 */
public class JavaAstParser implements AutoCloseable {
    private final TSParser parser;
    
    public JavaAstParser() {
        parser = new TSParser();
        parser.setLanguage(new TreeSitterJava());
    }
    
    /**
     * Parse a Java source file and return structured AST data.
     */
    public ParsedFile parse(Path filePath) throws IOException {
        String source = Files.readString(filePath);
        return parse(filePath, source);
    }
    
    /**
     * Parse Java source code with a known file path.
     */
    public ParsedFile parse(Path filePath, String source) {
        TSTree tree = parser.parseString(null, source);
        TSNode root = tree.getRootNode();
        return AstWalker.walk(filePath, root, source);
    }
    
    @Override
    public void close() {
        parser.close();
    }
}
