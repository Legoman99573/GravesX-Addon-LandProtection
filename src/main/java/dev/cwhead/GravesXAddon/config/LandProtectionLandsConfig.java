package dev.cwhead.GravesXAddon.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class LandProtectionLandsConfig {
    private static final String BASE_DIR = "plugins/GravesX/Addon/Land-Protection";
    private static final String FILE_NAME = "lands.yml";

    private final File file;
    private FileConfiguration cfg;

    public LandProtectionLandsConfig() {
        this.file = new File(BASE_DIR, FILE_NAME);
        ensureExistsWithDefaults();
        reload();
    }

    public void reload() {
        this.cfg = YamlConfiguration.loadConfiguration(file);
        applyMissingDefaults();
        saveQuietly();
    }

    public boolean isFlagEnabled(String flagId) {
        return cfg.getBoolean("flags." + flagId, true);
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

    public boolean rolesEnabled() {
        return cfg.getBoolean("roles.enabled", true);
    }

    public Set<String> allowedRolesForAction(String actionKey) {
        List<String> raw = cfg.getStringList("roles.allow." + actionKey);
        Set<String> out = new HashSet<>();
        if (raw != null) {
            for (String s : raw) {
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

            Map<String, Object> flags = new LinkedHashMap<>();
            flags.put("gravesx-grave-autoloot", true);
            flags.put("gravesx-grave-loot", true);
            flags.put("gravesx-grave-create", true);
            flags.put("gravesx-grave-teleport", true);
            flags.put("gravesx-grave-walkover", true);
            flags.put("gravesx-grave-projectile", true);
            flags.put("gravesx-grave-break", true);
            cfg.createSection("flags", flags);

            cfg.set("rules.allow-non-player", true);
            cfg.set("rules.invalid-location-allowed", true);
            cfg.set("rules.missing-flag-allowed", true);
            cfg.set("rules.wilderness-allowed", true);

            cfg.set("roles.enabled", true);
            cfg.set("roles.allow.create", Arrays.asList("OWNER", "TRUSTED"));
            cfg.set("roles.allow.teleport", Collections.emptyList());
            cfg.set("roles.allow.loot", Arrays.asList("OWNER", "TRUSTED"));
            cfg.set("roles.allow.autoloot", Arrays.asList("OWNER", "TRUSTED"));
            cfg.set("roles.allow.walkover", Arrays.asList("OWNER", "TRUSTED", "VISITOR"));
            cfg.set("roles.allow.projectile", Arrays.asList("OWNER", "TRUSTED"));
            cfg.set("roles.allow.break", Collections.singletonList("OWNER"));

            saveQuietly();
        }
    }

    private void applyMissingDefaults() {
        if (!cfg.isConfigurationSection("flags")) {
            Map<String, Object> flags = new LinkedHashMap<>();
            flags.put("gravesx-grave-autoloot", true);
            flags.put("gravesx-grave-loot", true);
            flags.put("gravesx-grave-create", true);
            flags.put("gravesx-grave-teleport", true);
            flags.put("gravesx-grave-walkover", true);
            flags.put("gravesx-grave-projectile", true);
            flags.put("gravesx-grave-break", true);
            cfg.createSection("flags", flags);
        }
        if (!cfg.isSet("rules.allow-non-player")) cfg.set("rules.allow-non-player", true);
        if (!cfg.isSet("rules.invalid-location-allowed")) cfg.set("rules.invalid-location-allowed", true);
        if (!cfg.isSet("rules.missing-flag-allowed")) cfg.set("rules.missing-flag-allowed", true);
        if (!cfg.isSet("rules.wilderness-allowed")) cfg.set("rules.wilderness-allowed", true);

        if (!cfg.isSet("roles.enabled")) cfg.set("roles.enabled", true);
        // action lists may be empty by design—don’t overwrite if present
        if (!cfg.isSet("roles.allow.create")) cfg.set("roles.allow.create", Arrays.asList("OWNER", "TRUSTED"));
        if (!cfg.isSet("roles.allow.teleport")) cfg.set("roles.allow.teleport", Collections.emptyList());
        if (!cfg.isSet("roles.allow.loot")) cfg.set("roles.allow.loot", Arrays.asList("OWNER", "TRUSTED"));
        if (!cfg.isSet("roles.allow.autoloot")) cfg.set("roles.allow.autoloot", Arrays.asList("OWNER", "TRUSTED"));
        if (!cfg.isSet("roles.allow.walkover")) cfg.set("roles.allow.walkover", Arrays.asList("OWNER", "TRUSTED", "VISITOR"));
        if (!cfg.isSet("roles.allow.projectile")) cfg.set("roles.allow.projectile", Arrays.asList("OWNER", "TRUSTED"));
        if (!cfg.isSet("roles.allow.break")) cfg.set("roles.allow.break", Collections.singletonList("OWNER"));
    }

    private void saveQuietly() {
        try {
            cfg.save(file);
        } catch (IOException ignored) {
            //ignored
        }
    }
}
