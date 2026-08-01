package com.repodna.config;

import com.repodna.exception.ConfigurationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.*;

public class ConfigManagerTest {

    @Test
    public void shouldThrowExceptionWhenConfigDoesNotExist(@TempDir Path tempDir) {
        ConfigManager manager = new ConfigManager(tempDir);
        assertThatThrownBy(manager::load)
            .isInstanceOf(ConfigurationException.class)
            .hasMessageContaining("does not exist");
    }

    @Test
    public void shouldCreateDirectoriesAndSaveConfig(@TempDir Path tempDir) {
        ConfigManager manager = new ConfigManager(tempDir);
        AppConfig config = new AppConfig(tempDir.toString(), "cache_dir", "reports_dir");

        manager.save(config);

        assertThat(Files.exists(manager.getConfigDir())).isTrue();
        assertThat(Files.exists(manager.getConfigFile())).isTrue();

        AppConfig loaded = manager.load();
        assertThat(loaded.getProjectPath()).isEqualTo(tempDir.toString());
        assertThat(loaded.getCachePath()).isEqualTo("cache_dir");
        assertThat(loaded.getOutputPath()).isEqualTo("reports_dir");
        assertThat(loaded.getVersion()).isEqualTo("1.0.0");
    }
}
