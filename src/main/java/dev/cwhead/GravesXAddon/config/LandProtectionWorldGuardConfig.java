package dev.cwhead.GravesXAddon.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class LandProtectionWorldGuardConfig {

    private static final String BASE_DIR = "plugins/GravesX/addon/Land-Protection";
    private static final String FILE_NAME = "worldguard.yml";

    private final File file;
    private FileConfiguration cfg;

    public LandProtectionWorldGuardConfig() {
        this.file = new File(BASE_DIR, FILE_NAME);
        ensureExistsWithCommentsTemplate();
        reload();
    }

    public void reload() {
        this.cfg = YamlConfiguration.loadConfiguration(file);
        applyMissingDefaultsInMemory();
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

    private void ensureExistsWithCommentsTemplate() {
        File dir = new File(BASE_DIR);
        if (!dir.exists()) dir.mkdirs();
        if (file.exists()) return;

        String template =
                "# Purpose:\n" +
                        "#   Control how the addon registers and evaluates WorldGuard (WG) StateFlags for GravesX.\n" +
                        "#   These flags are checked at a player’s location to decide whether an action is allowed.\n" +
                        "#\n" +
                        "# Evaluation overview (inside the addon):\n" +
                        "#   • For player entities:\n" +
                        "#       1) Query the WG StateFlag at the location for the specific action.\n" +
                        "#          - If the flag resolves to ALLOW → allow.\n" +
                        "#          - If the flag resolves to DENY → deny.\n" +
                        "#          - If the flag is absent/missing → follow rules.missing-flag-allowed.\n" +
                        "#       2) (Separately in your listener) membership may also be considered by your code,\n" +
                        "#          but that behavior is not configured here.\n" +
                        "#   • For non-player entities: see rules.allow-non-player.\n" +
                        "#\n" +
                        "# Notes:\n" +
                        "#   • Registering flags only creates them in WG’s registry if they don’t already exist.\n" +
                        "#     If a flag id already exists (e.g., from another plugin), the existing flag is used and\n" +
                        "#     its default cannot be changed here retroactively.\n" +
                        "#   • The per-flag defaults in this file apply only at registration time.\n" +
                        "#     Per-region values set in-game or by other plugins always take precedence at runtime.\n" +
                        "#   • After editing this file, reload the addon (or restart the server) to apply changes.\n" +
                        "\n" +
                        "flags:\n" +
                        "  # If false, the addon will NOT attempt to register custom WG flags at startup.\n" +
                        "  # Existing flags (if any) will still be looked up and used during evaluation.\n" +
                        "  register: true\n" +
                        "\n" +
                        "  # Default state used when creating a NEW StateFlag (only at registration time),\n" +
                        "  # unless overridden per-flag below in 'flags.list'.\n" +
                        "  # true → newly created StateFlags default to ALLOW\n" +
                        "  # false → newly created StateFlags default to DENY\n" +
                        "  default-state: true\n" +
                        "\n" +
                        "  # Per-flag default state overrides (applied only when a flag is first created).\n" +
                        "  # Keys are the exact flag ids. If WG already has a flag with that id, the existing\n" +
                        "  # one is reused and its default is left unchanged.\n" +
                        "  list:\n" +
                        "    gravesx-grave-autoloot: true\n" +
                        "    gravesx-grave-loot: true\n" +
                        "    gravesx-grave-create: true\n" +
                        "    gravesx-grave-teleport: true\n" +
                        "    gravesx-grave-walkover: true\n" +
                        "    gravesx-grave-projectile: true\n" +
                        "    gravesx-grave-break: true\n" +
                        "\n" +
                        "rules:\n" +
                        "  # Non-player entities (e.g., armor stands, projectiles, mobs) pass checks automatically.\n" +
                        "  # Set to false to enforce WG checks on non-player actors as well.\n" +
                        "  allow-non-player: true\n" +
                        "\n" +
                        "  # If the Location (or world) is invalid/unavailable:\n" +
                        "  #   true → allow (fail-open)\n" +
                        "  #   false → deny (fail-closed)\n" +
                        "  invalid-location-allowed: true\n" +
                        "\n" +
                        "  # If a required flag is missing (not registered, could not be resolved, or failed to load):\n" +
                        "  #   true → allow (fail-open)\n" +
                        "  #   false → deny (fail-closed)\n" +
                        "  missing-flag-allowed: true\n";

        try (FileWriter fw = new FileWriter(file)) {
            fw.write(template);
        } catch (IOException ignored) {
            // ignored
        }
    }

    private void applyMissingDefaultsInMemory() {
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

    @SuppressWarnings("unused")
    private void saveQuietly() {
        try {
            cfg.save(file);
        } catch (IOException ignored) {
            // ignored
        }
    }
}