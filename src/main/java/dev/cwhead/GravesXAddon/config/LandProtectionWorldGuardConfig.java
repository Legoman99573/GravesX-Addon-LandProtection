package dev.cwhead.GravesXAddon.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class LandProtectionWorldGuardConfig {

    private static final String BASE_DIR = "plugins/GravesX/Addon/Land-Protection";
    private static final String FILE_NAME = "worldguard.yml";

    private final File file;
    private FileConfiguration cfg;

    public LandProtectionWorldGuardConfig() {
        this.file = new File(BASE_DIR, FILE_NAME);
        ensureExistsWithDefaults();
        reload();
    }

    public void reload() {
        this.cfg = YamlConfiguration.loadConfiguration(file);
        applyMissingDefaults();
        saveQuietly();
    }

    public boolean shouldRegisterFlags() {
        return cfg.getBoolean("flags.register", true);
    }

    public boolean defaultFlagState() {
        return cfg.getBoolean("flags.default-state", true);
    }

    public Map<String, Boolean> flagDefaults() {
        Map<String, Boolean> out = new LinkedHashMap<>();
        if (cfg.isConfigurationSection("flags.list")) {
            for (String key : Objects.requireNonNull(cfg.getConfigurationSection("flags.list")).getKeys(false)) {
                out.put(key, cfg.getBoolean("flags.list." + key));
            }
        }
        return out;
    }

    public boolean allowNonPlayer() {
        return cfg.getBoolean("rules.allow-non-player", true);
    }

    public boolean invalidLocationAllowed() {
        return cfg.getBoolean("rules.invalid-location-allowed", true);
    }

    public boolean missingFlagAllowed() {
        return cfg.getBoolean("rules.missing-flag-allowed", true);
    }

    private void ensureExistsWithDefaults() {
        File dir = new File(BASE_DIR);
        if (!dir.exists()) dir.mkdirs();

        if (!file.exists()) {
            this.cfg = new YamlConfiguration();
            cfg.set("flags.register", true);
            cfg.set("flags.default-state", true);

            Map<String, Object> list = new LinkedHashMap<>();
            list.put("gravesx-grave-autoloot", true);
            list.put("gravesx-grave-loot", true);
            list.put("gravesx-grave-create", true);
            list.put("gravesx-grave-teleport", true);
            list.put("gravesx-grave-walkover", true);
            list.put("gravesx-grave-projectile", true);
            list.put("gravesx-grave-break", true);
            cfg.createSection("flags.list", list);

            cfg.set("rules.allow-non-player", true);
            cfg.set("rules.invalid-location-allowed", true);
            cfg.set("rules.missing-flag-allowed", true);

            saveQuietly();
        }
    }

    private void applyMissingDefaults() {
        if (!cfg.isSet("flags.register")) cfg.set("flags.register", true);
        if (!cfg.isSet("flags.default-state")) cfg.set("flags.default-state", true);
        if (!cfg.isConfigurationSection("flags.list")) {
            Map<String, Object> list = new LinkedHashMap<>();
            list.put("gravesx-grave-autoloot", true);
            list.put("gravesx-grave-loot", true);
            list.put("gravesx-grave-create", true);
            list.put("gravesx-grave-teleport", true);
            list.put("gravesx-grave-walkover", true);
            list.put("gravesx-grave-projectile", true);
            list.put("gravesx-grave-break", true);
            cfg.createSection("flags.list", list);
        }
        if (!cfg.isSet("rules.allow-non-player")) cfg.set("rules.allow-non-player", true);
        if (!cfg.isSet("rules.invalid-location-allowed")) cfg.set("rules.invalid-location-allowed", true);
        if (!cfg.isSet("rules.missing-flag-allowed")) cfg.set("rules.missing-flag-allowed", true);
    }

    private void saveQuietly() {
        try {
            cfg.save(file);
        } catch (IOException ignored) {
            //ignored
        }
    }
}
