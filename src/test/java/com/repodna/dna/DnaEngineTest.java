package com.repodna.dna;

import com.repodna.discovery.model.*;
import com.repodna.graph.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

public class DnaEngineTest {

    @Test
    public void shouldAnalyzeAndGenerateReportsEndToEnd(@TempDir Path tempDir) throws IOException {
        Path srcRoot = tempDir.resolve("src/main/java");
        Files.createDirectories(srcRoot.resolve("com/example"));

        String userClass = 
            "package com.example;\n" +
            "public class User {\n" +
            "    private String name;\n" +
            "    public String getName() { return this.name; }\n" +
            "}";

        String serviceClass = 
            "package com.example;\n" +
            "import org.springframework.stereotype.Service;\n" +
            "@Service\n" +
            "public class UserService {\n" +
            "    private final User user;\n" +
            "    public UserService(User user) {\n" +
            "        this.user = user;\n" +
            "    }\n" +
            "}";

        Files.writeString(srcRoot.resolve("com/example/User.java"), userClass);
        Files.writeString(srcRoot.resolve("com/example/UserService.java"), serviceClass);

        // Run DnaEngine end-to-end
        DnaProfile profile = DnaEngine.analyze(tempDir);

        assertThat(profile.scannerResult()).isNotNull();
        assertThat(profile.projectModel()).isNotNull();
        assertThat(profile.graph()).isNotNull();
        assertThat(profile.patterns()).isNotEmpty();
        assertThat(profile.summary()).isNotNull();

        // Naming suffix controller pattern shouldn't match (none exists), but service pattern should match
        Pattern serviceNaming = profile.patterns().stream()
            .filter(p -> "naming-suffix-service".equals(p.id()))
            .findFirst()
            .orElseThrow();
        assertThat(serviceNaming.confidence().score()).isEqualTo(1.0);

        // Generate Reports
        DnaReportGenerator.generateReports(profile, tempDir);

        assertThat(tempDir.resolve("REPO_DNA.md")).exists();
        assertThat(tempDir.resolve("AGENTS.md")).exists();
        assertThat(tempDir.resolve("ARCHITECTURE.md")).exists();
        assertThat(tempDir.resolve("PROJECT_RULES.md")).exists();

        String repoDnaContent = Files.readString(tempDir.resolve("REPO_DNA.md"));
        assertThat(repoDnaContent).contains("Repository Engineering DNA Profile");
        assertThat(repoDnaContent).contains("naming-suffix-service");

        String agentsContent = Files.readString(tempDir.resolve("AGENTS.md"));
        assertThat(agentsContent).contains("AI Agent Coding Instructions");
        assertThat(agentsContent).contains("DOs and DONTs");
    }
}
