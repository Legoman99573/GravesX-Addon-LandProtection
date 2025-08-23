package dev.cwhead.GravesXAddon.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Handles the addon-wide config.yml
 */
public class LandProtectionMainConfig {

    private static final String BASE_DIR = "plugins/GravesX/addon/Land-Protection";
    private static final String FILE_NAME = "config.yml";

    private final File file;
    private FileConfiguration cfg;

    public LandProtectionMainConfig() {
        this.file = new File(BASE_DIR, FILE_NAME);
        ensureExistsWithCommentsTemplate();
        reload();
    }

    public void reload() {
        this.cfg = YamlConfiguration.loadConfiguration(file);
        applyMissingDefaultsInMemory();
    }

    public boolean metricsEnabled() {
        return cfg.getBoolean("metrics", true);
    }

    private void ensureExistsWithCommentsTemplate() {
        File dir = new File(BASE_DIR);
        if (!dir.exists()) dir.mkdirs();
        if (file.exists()) return;

        String template =
                "# Purpose:\n" +
                        "#   General settings for the GravesX Land-Protection addon.\n" +
                        "#   Edit these values and restart or reload the plugin to apply.\n" +
                        "#\n" +
                        "metrics: true   # Enable anonymous usage metrics reporting to bStats\n";

        try (FileWriter fw = new FileWriter(file)) {
            fw.write(template);
        } catch (IOException ignored) {
            // ignored
        }
    }

    private void applyMissingDefaultsInMemory() {
        if (!cfg.isSet("metrics"))
            cfg.set("metrics", true);
    }

    @SuppressWarnings("unused")
    private void saveQuietly() {
        try {
            cfg.save(file);
        } catch (IOException ignored) {
            // ignored
        }
    }
}
