package dev.cwhead.GravesXAddon.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class LandProtectionTownyConfig {

    private static final String BASE_DIR = "plugins/GravesX/Addon/Land-Protection";
    private static final String FILE_NAME = "towny.yml";

    private final File file;
    private FileConfiguration cfg;

    public LandProtectionTownyConfig() {
        this.file = new File(BASE_DIR, FILE_NAME);
        ensureExistsWithDefaults();
        reload();
    }

    public void reload() {
        this.cfg = YamlConfiguration.loadConfiguration(file);
        applyMissingDefaults();
        saveQuietly();
    }

    public Set<String> getAllyAllowedTypes() {
        return toUpperEnumIdSet(cfg.getStringList("ally-allowed-types"));
    }

    public Set<String> getOutsiderAllowedTypes() {
        return toUpperEnumIdSet(cfg.getStringList("outsider-allowed-types"));
    }

    public boolean isWildernessAllowed() {
        return cfg.getBoolean("rules.wilderness-allowed", true);
    }

    public boolean isResidentsAlwaysAllowed() {
        return cfg.getBoolean("rules.residents-always-allowed", true);
    }

    private static Set<String> toUpperEnumIdSet(List<String> in) {
        Set<String> out = new HashSet<>();
        if (in != null) {
            for (String s : in) {
                if (s != null && !s.isEmpty()) out.add(s.trim().toUpperCase(Locale.ROOT));
            }
        }
        return out;
    }

    private void ensureExistsWithDefaults() {
        File dir = new File(BASE_DIR);
        if (!dir.exists()) dir.mkdirs();

        if (!file.exists()) {
            this.cfg = new YamlConfiguration();
            cfg.set("ally-allowed-types", Arrays.asList("EMBASSY", "ARENA"));
            cfg.set("outsider-allowed-types", Collections.emptyList());
            cfg.set("rules.wilderness-allowed", true);
            cfg.set("rules.residents-always-allowed", true);
            saveQuietly();
        }
    }

    private void applyMissingDefaults() {
        if (!cfg.isSet("ally-allowed-types"))
            cfg.set("ally-allowed-types", Arrays.asList("EMBASSY", "ARENA"));

        if (!cfg.isSet("outsider-allowed-types"))
            cfg.set("outsider-allowed-types", Collections.emptyList());

        if (!cfg.isSet("rules.wilderness-allowed"))
            cfg.set("rules.wilderness-allowed", true);

        if (!cfg.isSet("rules.residents-always-allowed"))
            cfg.set("rules.residents-always-allowed", true);
    }

    private void saveQuietly() {
        try {
            cfg.save(file);
        } catch (IOException ignored) {
            //ignored
        }
    }
}
