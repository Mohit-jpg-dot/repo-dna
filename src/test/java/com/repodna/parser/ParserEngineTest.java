package com.repodna.parser;

import com.repodna.parser.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

public class ParserEngineTest {

    @Test
    public void shouldParseSimpleJavaClassAndResolveTypes(@TempDir Path tempDir) throws IOException {
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
            "    public String getUserName() {\n" +
            "        return user.getName();\n" +
            "    }\n" +
            "}";

        Path userFile = srcRoot.resolve("com/example/User.java");
        Path serviceFile = srcRoot.resolve("com/example/UserService.java");
        Files.writeString(userFile, userClass);
        Files.writeString(serviceFile, serviceClass);

        ParserEngine engine = new ParserEngine(List.of(srcRoot));
        ProjectModel project = engine.parse("TestProject", List.of(userFile, serviceFile));

        assertThat(project.name()).isEqualTo("TestProject");
        assertThat(project.packages()).hasSize(1);
        
        PackageModel pkg = project.packages().get(0);
        assertThat(pkg.name()).isEqualTo("com.example");
        assertThat(pkg.classes()).hasSize(2);

        ClassModel userModel = pkg.classes().stream()
            .filter(c -> "User".equals(c.name()))
            .findFirst()
            .orElseThrow();

        ClassModel serviceModel = pkg.classes().stream()
            .filter(c -> "UserService".equals(c.name()))
            .findFirst()
            .orElseThrow();

        assertThat(userModel.fields()).hasSize(1);
        assertThat(userModel.fields().get(0).name()).isEqualTo("name");
        assertThat(userModel.fields().get(0).type().qualifiedName()).isEqualTo("java.lang.String");

        assertThat(serviceModel.fields().get(0).type().qualifiedName()).isEqualTo("com.example.User");
        assertThat(serviceModel.fields().get(0).type().isResolved()).isTrue();

        SpringDetector.SpringMeta springMeta = SpringDetector.detect(serviceModel);
        assertThat(springMeta.isSpringManaged()).isTrue();
        assertThat(springMeta.stereotypes()).contains("Service");
        assertThat(springMeta.injectionStyle()).isEqualTo("Constructor Injection");

        MethodModel getUserNameMethod = serviceModel.methods().stream()
            .filter(m -> "getUserName".equals(m.name()))
            .findFirst()
            .orElseThrow();
        assertThat(getUserNameMethod.calls()).hasSize(1);
        assertThat(getUserNameMethod.calls().get(0).methodName()).isEqualTo("getName");
        assertThat(getUserNameMethod.calls().get(0).calleeTypeFqn()).isEqualTo("com.example.User");
    }

    @Test
    public void shouldParseEnumsRecordsAndInterfaces(@TempDir Path tempDir) throws IOException {
        Path srcRoot = tempDir.resolve("src/main/java");
        Files.createDirectories(srcRoot.resolve("com/example"));

        String interfaceCode = 
            "package com.example;\n" +
            "public interface Repository {\n" +
            "    void save();\n" +
            "}";

        String enumCode = 
            "package com.example;\n" +
            "public enum Role {\n" +
            "    ADMIN, USER;\n" +
            "}";

        String recordCode = 
            "package com.example;\n" +
            "public record Config(String url, int port) implements Repository {\n" +
            "    public void save() {}\n" +
            "}";

        Path interfaceFile = srcRoot.resolve("com/example/Repository.java");
        Path enumFile = srcRoot.resolve("com/example/Role.java");
        Path recordFile = srcRoot.resolve("com/example/Config.java");

        Files.writeString(interfaceFile, interfaceCode);
        Files.writeString(enumFile, enumCode);
        Files.writeString(recordFile, recordCode);

        ParserEngine engine = new ParserEngine(List.of(srcRoot));
        ProjectModel project = engine.parse("TestProject", List.of(interfaceFile, enumFile, recordFile));

        PackageModel pkg = project.packages().get(0);

        assertThat(pkg.interfaces()).hasSize(1);
        assertThat(pkg.interfaces().get(0).name()).isEqualTo("Repository");

        assertThat(pkg.enums()).hasSize(1);
        assertThat(pkg.enums().get(0).name()).isEqualTo("Role");
        assertThat(pkg.enums().get(0).entries()).containsExactly("ADMIN", "USER");

        assertThat(pkg.records()).hasSize(1);
        assertThat(pkg.records().get(0).name()).isEqualTo("Config");
        assertThat(pkg.records().get(0).components()).hasSize(2);
        assertThat(pkg.records().get(0).components().get(0).name()).isEqualTo("url");
    }
}
