package dev.cwhead.GravesXAddon.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class LandProtectionGriefPreventionConfig {

    private static final String BASE_DIR = "plugins/GravesX/Addon/Land-Protection";
    private static final String FILE_NAME = "griefprevention.yml";

    private final File file;
    private FileConfiguration cfg;

    public enum EvalOrder {
        FLAGS_THEN_TRUST,
        TRUST_THEN_FLAGS
    }

    public LandProtectionGriefPreventionConfig() {
        this.file = new File(BASE_DIR, FILE_NAME);
        ensureExistsWithDefaults();
        reload();
    }

    public void reload() {
        this.cfg = YamlConfiguration.loadConfiguration(file);
        applyMissingDefaults();
        saveQuietly();
    }

    public boolean useFlags() {
        return cfg.getBoolean("flags.use-flags", true);
    }

    public boolean useGPFlags() {
        return cfg.getBoolean("flags.use-gpflags", true);
    }

    public boolean registerFlags() {
        return cfg.getBoolean("flags.register.flags", true);
    }

    public boolean registerGPFlags() {
        return cfg.getBoolean("flags.register.gpflags", true);
    }

    public boolean flagDefault(String id, boolean fallback) {
        String path = "flags.defaults." + id;
        return cfg.isSet(path) ? cfg.getBoolean(path) : fallback;
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

    public boolean wildernessAllowed() {
        return cfg.getBoolean("rules.wilderness-allowed", true);
    }

    public boolean ownerBypass() {
        return cfg.getBoolean("rules.owner-bypass", true);
    }

    public boolean flagsDenyOverrides() {
        return cfg.getBoolean("rules.flags-deny-overrides", true);
    }

    public EvalOrder evaluationOrder() {
        String s = cfg.getString("rules.evaluation-order", "FLAGS_THEN_TRUST").toUpperCase(Locale.ROOT);
        try {
            return EvalOrder.valueOf(s);
        } catch (IllegalArgumentException e) {
            return EvalOrder.FLAGS_THEN_TRUST;
        }
    }

    public String trustLevelFor(String actionKey) {
        return cfg.getString("trust.levels." + actionKey);
    }

    private void ensureExistsWithDefaults() {
        File dir = new File(BASE_DIR);
        if (!dir.exists()) dir.mkdirs();
        if (!file.exists()) {
            this.cfg = new YamlConfiguration();

            cfg.set("flags.use-flags", true);
            cfg.set("flags.use-gpflags", true);
            cfg.set("flags.register.flags", true);
            cfg.set("flags.register.gpflags", true);

            Map<String, Object> defaults = new LinkedHashMap<>();
            defaults.put("gravesx-grave-create", false);
            defaults.put("gravesx-grave-teleport", true);
            defaults.put("gravesx-grave-loot", true);
            defaults.put("gravesx-grave-autoloot", true);
            defaults.put("gravesx-grave-walkover", true);
            defaults.put("gravesx-grave-projectile", false);
            defaults.put("gravesx-grave-break", false);
            cfg.createSection("flags.defaults", defaults);

            cfg.set("rules.allow-non-player", true);
            cfg.set("rules.invalid-location-allowed", true);
            cfg.set("rules.missing-flag-allowed", true);
            cfg.set("rules.wilderness-allowed", true);
            cfg.set("rules.owner-bypass", true);
            cfg.set("rules.evaluation-order", "FLAGS_THEN_TRUST");
            cfg.set("rules.flags-deny-overrides", true);

            cfg.set("trust.levels.create", "BUILDER");
            cfg.set("trust.levels.teleport", "ACCESSOR");
            cfg.set("trust.levels.loot", "CONTAINER");
            cfg.set("trust.levels.autoloot", "CONTAINER");
            cfg.set("trust.levels.walkover", "ACCESSOR");
            cfg.set("trust.levels.projectile", "ACCESSOR");
            cfg.set("trust.levels.break", "BUILDER");

            saveQuietly();
        }
    }

    private void applyMissingDefaults() {
        if (!cfg.isSet("flags.use-flags"))
            cfg.set("flags.use-flags", true);

        if (!cfg.isSet("flags.use-gpflags"))
            cfg.set("flags.use-gpflags", true);

        if (!cfg.isSet("flags.register.flags"))
            cfg.set("flags.register.flags", true);

        if (!cfg.isSet("flags.register.gpflags"))
            cfg.set("flags.register.gpflags", true);

        if (!cfg.isConfigurationSection("flags.defaults")) {
            Map<String, Object> defaults = new LinkedHashMap<>();
            defaults.put("gravesx-grave-create", false);
            defaults.put("gravesx-grave-teleport", true);
            defaults.put("gravesx-grave-loot", true);
            defaults.put("gravesx-grave-autoloot", true);
            defaults.put("gravesx-grave-walkover", true);
            defaults.put("gravesx-grave-projectile", false);
            defaults.put("gravesx-grave-break", false);
            cfg.createSection("flags.defaults", defaults);
        }

        if (!cfg.isSet("rules.allow-non-player"))
            cfg.set("rules.allow-non-player", true);

        if (!cfg.isSet("rules.invalid-location-allowed"))
            cfg.set("rules.invalid-location-allowed", true);

        if (!cfg.isSet("rules.missing-flag-allowed"))
            cfg.set("rules.missing-flag-allowed", true);

        if (!cfg.isSet("rules.wilderness-allowed"))
            cfg.set("rules.wilderness-allowed", true);

        if (!cfg.isSet("rules.owner-bypass"))
            cfg.set("rules.owner-bypass", true);

        if (!cfg.isSet("rules.evaluation-order"))
            cfg.set("rules.evaluation-order", "FLAGS_THEN_TRUST");

        if (!cfg.isSet("rules.flags-deny-overrides"))
            cfg.set("rules.flags-deny-overrides", true);

        if (!cfg.isSet("trust.levels.create"))
            cfg.set("trust.levels.create", "BUILDER");

        if (!cfg.isSet("trust.levels.teleport"))
            cfg.set("trust.levels.teleport", "ACCESSOR");

        if (!cfg.isSet("trust.levels.loot"))
            cfg.set("trust.levels.loot", "CONTAINER");

        if (!cfg.isSet("trust.levels.autoloot"))
            cfg.set("trust.levels.autoloot", "CONTAINER");

        if (!cfg.isSet("trust.levels.walkover"))
            cfg.set("trust.levels.walkover", "ACCESSOR");

        if (!cfg.isSet("trust.levels.projectile"))
            cfg.set("trust.levels.projectile", "ACCESSOR");

        if (!cfg.isSet("trust.levels.break"))
            cfg.set("trust.levels.break", "BUILDER");
    }

    private void saveQuietly() {
        try {
            cfg.save(file);
        } catch (IOException ignored) {
            //ignored
        }
    }
}
