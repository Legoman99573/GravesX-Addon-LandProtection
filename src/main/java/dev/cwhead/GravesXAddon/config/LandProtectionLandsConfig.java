package dev.cwhead.GravesXAddon.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class LandProtectionLandsConfig {
    private static final String BASE_DIR = "plugins/GravesX/addon/Land-Protection";
    private static final String FILE_NAME = "lands.yml";

    private final File file;
    private FileConfiguration cfg;

    public LandProtectionLandsConfig() {
        this.file = new File(BASE_DIR, FILE_NAME);
        ensureExistsWithCommentsTemplate();
        reload();
    }

    public void reload() {
        this.cfg = YamlConfiguration.loadConfiguration(file);
        applyMissingDefaultsInMemory();
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

    private void ensureExistsWithCommentsTemplate() {
        File dir = new File(BASE_DIR);
        if (!dir.exists()) dir.mkdirs();
        if (file.exists()) return;

        String template =
                "# Purpose:\n" +
                        "#   This file controls how the addon talks to the Lands plugin:\n" +
                        "#   - Which custom GravesX flags are registered and checked\n" +
                        "#   - What to do when context is missing (fail-open vs fail-closed)\n" +
                        "#   - (Optional) Extra gating by Lands roles/“buckets” per action\n" +
                        "#\n" +
                        "#   ACTION KEYS used in this file:\n" +
                        "#     create, teleport, loot, autoloot, walkover, projectile, break\n" +
                        "#\n" +
                        "# EVALUATION OVERVIEW (for a player at a location inside a Land/Area):\n" +
                        "#   1) Flag check (if the flag for that action is enabled & registered):\n" +
                        "#        - If the flag explicitly ALLOWS → allow.\n" +
                        "#        - If the flag explicitly DENIES → deny.\n" +
                        "#        - If the flag is missing/disabled → follow rules.missing-flag-allowed.\n" +
                        "#   2) (Optional) Role gating (roles.enabled = true):\n" +
                        "#        - If an action has a non-empty allow-list → player must match at least one listed bucket/role.\n" +
                        "#        - If the list is empty or the action key is absent → no extra role restriction is applied.\n" +
                        "#   3) Outside of any land (wilderness) → follow rules.wilderness-allowed.\n" +
                        "#\n" +
                        "# Toggle registration of each GravesX role flag in Lands (by flag id).\n" +
                        "# Disable a flag if you never want that action to be controlled by Lands flags.\n" +
                        "# If you disable a flag, behavior falls back to 'rules.missing-flag-allowed' for that action.\n" +
                        "flags:\n" +
                        "  gravesx-grave-autoloot: true\n" +
                        "  gravesx-grave-loot: true\n" +
                        "  gravesx-grave-create: true\n" +
                        "  gravesx-grave-teleport: true\n" +
                        "  gravesx-grave-walkover: true\n" +
                        "  gravesx-grave-projectile: true\n" +
                        "  gravesx-grave-break: true\n" +
                        "\n" +
                        "rules:\n" +
                        "  # Non-player actors (e.g., armor stands, item projectiles, mobs) pass checks automatically.\n" +
                        "  # Set to false to also enforce on non-players.\n" +
                        "  allow-non-player: true\n" +
                        "\n" +
                        "  # If the location is invalid (null world, unloaded, etc.), treat as allowed (fail-open).\n" +
                        "  # Set to false to fail-closed (deny when context is missing).\n" +
                        "  invalid-location-allowed: true\n" +
                        "\n" +
                        "  # If the relevant flag is disabled or cannot be resolved at runtime:\n" +
                        "  #  - true  → treat as allowed (fail-open)\n" +
                        "  #  - false → treat as denied (fail-closed)\n" +
                        "  missing-flag-allowed: true\n" +
                        "\n" +
                        "  # When no land/area is present at the location (wilderness):\n" +
                        "  #  - true  → allow the action in wilderness\n" +
                        "  #  - false → deny in wilderness\n" +
                        "  wilderness-allowed: true\n" +
                        "\n" +
                        "# Matching logic:\n" +
                        "#   - A player matches OWNER if they own the Land/Area.\n" +
                        "#   - A player matches TRUSTED if they are trusted in the Land/Area.\n" +
                        "#   - A player matches VISITOR if they are neither owner nor trusted (i.e., generic visitor).\n" +
                        "#   - Named roles are matched by the player’s assigned Lands role at that location.\n" +
                        "#\n" +
                        "# If a list is empty ([]) or the action key is missing entirely, that action has NO additional role gating.\n" +
                        "roles:\n" +
                        "  # Master switch for role gating (applies to all actions below).\n" +
                        "  enabled: true\n" +
                        "\n" +
                        "  # Allow-lists per action. Player must match at least ONE entry in the list to pass.\n" +
                        "  # Examples below are sensible defaults; adjust to your policy.\n" +
                        "  allow:\n" +
                        "    # Creating a grave usually needs build-level access.\n" +
                        "    create: [OWNER, TRUSTED]\n" +
                        "    # Teleporting to a grave is often harmless; leave open by default.\n" +
                        "    teleport: []\n" +
                        "    # Looting & autolooting are inventory-affecting; limit to trusted by default.\n" +
                        "    loot: [OWNER, TRUSTED]\n" +
                        "    autoloot: [OWNER, TRUSTED]\n" +
                        "    # Walking over is benign; include visitors by default.\n" +
                        "    walkover: [OWNER, TRUSTED, VISITOR]\n" +
                        "    # Projectile interactions can be destructive; restrict by default.\n" +
                        "    projectile: [OWNER, TRUSTED]\n" +
                        "    # Breaking a grave is destructive; owner by default.\n" +
                        "    break: [OWNER]\n";

        try (FileWriter fw = new FileWriter(file)) {
            fw.write(template);
        } catch (IOException ignored) {
            // ignored
        }
    }

    private void applyMissingDefaultsInMemory() {
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

        if (!cfg.isSet("roles.allow.create"))
            cfg.set("roles.allow.create", Arrays.asList("OWNER", "TRUSTED"));

        if (!cfg.isSet("roles.allow.teleport"))
            cfg.set("roles.allow.teleport", Collections.emptyList());

        if (!cfg.isSet("roles.allow.loot"))
            cfg.set("roles.allow.loot", Arrays.asList("OWNER", "TRUSTED"));

        if (!cfg.isSet("roles.allow.autoloot"))
            cfg.set("roles.allow.autoloot", Arrays.asList("OWNER", "TRUSTED"));

        if (!cfg.isSet("roles.allow.walkover"))
            cfg.set("roles.allow.walkover", Arrays.asList("OWNER", "TRUSTED", "VISITOR"));

        if (!cfg.isSet("roles.allow.projectile"))
            cfg.set("roles.allow.projectile", Arrays.asList("OWNER", "TRUSTED"));

        if (!cfg.isSet("roles.allow.break"))
            cfg.set("roles.allow.break", Collections.singletonList("OWNER"));
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