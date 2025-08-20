package dev.cwhead.GravesXAddon.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class LandProtectionGriefPreventionConfig {

    private static final String BASE_DIR = "plugins/GravesX/addon/Land-Protection";
    private static final String FILE_NAME = "griefprevention.yml";

    private final File file;
    private FileConfiguration cfg;

    public enum EvalOrder {
        FLAGS_THEN_TRUST,
        TRUST_THEN_FLAGS
    }

    public LandProtectionGriefPreventionConfig() {
        this.file = new File(BASE_DIR, FILE_NAME);
        ensureExistsWithCommentsTemplate();
        reload();
    }

    public void reload() {
        this.cfg = YamlConfiguration.loadConfiguration(file);
        applyMissingDefaultsInMemory();
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
        String s = cfg.getString("rules.evaluation-order", "FLAGS_THEN_TRUST");
        try {
            return EvalOrder.valueOf(s.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return EvalOrder.FLAGS_THEN_TRUST;
        }
    }

    public String trustLevelFor(String actionKey) {
        return cfg.getString("trust.levels." + actionKey);
    }

    private void ensureExistsWithCommentsTemplate() {
        File dir = new File(BASE_DIR);
        if (!dir.exists()) dir.mkdirs();
        if (file.exists()) return;

        String template =
                "# Purpose:\n" +
                        "#   This file controls how the addon works with GriefPrevention (GP) and its flag ecosystems:\n" +
                        "#   - \"Flags\" (successor API; plugin name: Flags)\n" +
                        "#   - \"GPFlags\" (legacy; plugin name: GPFlags or GriefPreventionFlags)\n" +
                        "#\n" +
                        "# Evaluation overview for a player acting inside a GP claim:\n" +
                        "#   1) Depending on 'evaluation-order', evaluate Flags/GPFlags first or GP trust first.\n" +
                        "#   2) Flags/GPFlags: explicit ALLOW → allow; explicit DENY → deny; no setting → see rules.missing-flag-allowed.\n" +
                        "#   3) Trust fallback (ACCESSOR/CONTAINER/BUILDER), action-specific via trust.levels.*.\n" +
                        "#   4) If the location is wilderness → follow rules.wilderness-allowed.\n" +
                        "#   5) Owner bypass (if enabled) allows action regardless of flags/trust.\n" +
                        "#\n" +
                        "\n" +
                        "# Control whether to use and/or register flag integrations.\n" +
                        "flags:\n" +
                        "  # Use the \"Flags\" successor API if the plugin is present (plugin id: Flags).\n" +
                        "  # Set to false to ignore the Flags plugin even if installed.\n" +
                        "  use-flags: true\n" +
                        "\n" +
                        "  # Use the legacy GPFlags API if a legacy plugin is present (GPFlags or GriefPreventionFlags).\n" +
                        "  # Set to false to ignore legacy GPFlags even if installed.\n" +
                        "  use-gpflags: true\n" +
                        "\n" +
                        "  # Attempt to register flags with each API (only if the corresponding 'use-*' is true AND the plugin is present).\n" +
                        "  # Disable if you prefer to pre-create flags or manage them externally.\n" +
                        "  register:\n" +
                        "    flags: true # Register with successor \"Flags\"\n" +
                        "    gpflags: true # Register with legacy \"GPFlags\"\n" +
                        "\n" +
                        "  # Default states used ONLY when registering via successor \"Flags\".\n" +
                        "  # GPFlags does not support defaults on register—admins must set values in-game or via its config.\n" +
                        "  #\n" +
                        "  # Meaning of values here (Flags only):\n" +
                        "  #   true  → default ALLOW unless overridden per-claim\n" +
                        "  #   false → default DENY  unless overridden per-claim\n" +
                        "  # Remove or comment a key to let the Flags plugin’s own default behavior apply.\n" +
                        "  defaults:\n" +
                        "    gravesx-grave-create: false # Creating a grave (typically restrict by default)\n" +
                        "    gravesx-grave-teleport: true # Teleport to grave\n" +
                        "    gravesx-grave-loot: true # Open/loot grave\n" +
                        "    gravesx-grave-autoloot: true # Auto-loot on interaction\n" +
                        "    gravesx-grave-walkover: true # Walking over a grave\n" +
                        "    gravesx-grave-projectile: false # Projectile impacting a grave (often restrict)\n" +
                        "    gravesx-grave-break: false # Breaking a grave (often restrict)\n" +
                        "\n" +
                        "rules:\n" +
                        "  # Non-player entities (armor stands, projectiles, mobs) pass checks automatically.\n" +
                        "  # Set to false to enforce checks for non-players too.\n" +
                        "  allow-non-player: true\n" +
                        "\n" +
                        "  # If the location/world context is invalid (e.g., null/unloaded), treat as allowed (fail-open).\n" +
                        "  # Set to false to fail-closed (deny when context is missing).\n" +
                        "  invalid-location-allowed: true\n" +
                        "\n" +
                        "  # If flag integrations are unavailable/disabled or a flag cannot be resolved:\n" +
                        "  #   true  → treat as allowed (fail-open)\n" +
                        "  #   false → treat as denied  (fail-closed)\n" +
                        "  missing-flag-allowed: true\n" +
                        "\n" +
                        "  # When no claim is present at the location (wilderness):\n" +
                        "  #   true  → allow the action\n" +
                        "  #   false → deny in wilderness\n" +
                        "  wilderness-allowed: true\n" +
                        "\n" +
                        "  # If the acting player is the owner of the claim, bypass all checks.\n" +
                        "  owner-bypass: true\n" +
                        "\n" +
                        "  # Order of evaluation between flags and trust:\n" +
                        "  #   FLAGS_THEN_TRUST → evaluate Flags/GPFlags first; if UNDEFINED, fall back to trust\n" +
                        "  #   TRUST_THEN_FLAGS → evaluate GP trust first; if not sufficient, consult Flags/GPFlags\n" +
                        "  evaluation-order: FLAGS_THEN_TRUST\n" +
                        "\n" +
                        "  # While reading flags, if any explicit DENY is encountered:\n" +
                        "  #   true  → treat as DENY even if another flag might ALLOW\n" +
                        "  #   false → do not give DENY precedence (rare)\n" +
                        "  flags-deny-overrides: true\n" +
                        "\n" +
                        "trust:\n" +
                        "  # Per-action fallback trust level used when evaluation reaches GP trust checks.\n" +
                        "  # Valid values:\n" +
                        "  #   ACCESSOR  → minimal access (use buttons/doors), also satisfied by CONTAINER/BUILDER/MANAGER\n" +
                        "  #   CONTAINER → open containers, also satisfied by BUILDER/MANAGER\n" +
                        "  #   BUILDER   → place/break, also satisfied by MANAGER\n" +
                        "  #\n" +
                        "  # Tip: Pick stricter levels for destructive actions (e.g., create/break).\n" +
                        "  levels:\n" +
                        "    create: BUILDER\n" +
                        "    teleport: ACCESSOR\n" +
                        "    loot: CONTAINER\n" +
                        "    autoloot: CONTAINER\n" +
                        "    walkover: ACCESSOR\n" +
                        "    projectile: ACCESSOR\n" +
                        "    break: BUILDER\n";

        try (FileWriter fw = new FileWriter(file)) {
            fw.write(template);
        } catch (IOException ignored) {
            // ignored
        }
    }

    private void applyMissingDefaultsInMemory() {
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

    @SuppressWarnings("unused")
    private void saveQuietly() {
        try {
            cfg.save(file);
        } catch (IOException ignored) {
            // ignored
        }
    }
}
