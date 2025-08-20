package dev.cwhead.GravesXAddon.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Locale;

public class LandProtectionGriefDefenderConfig {

    private static final String BASE_DIR = "plugins/GravesX/addon/Land-Protection";
    private static final String FILE_NAME = "griefdefender.yml";

    public enum EvalOrder {
        FLAGS_THEN_TRUST,
        TRUST_THEN_FLAGS
    }

    private final File file;
    private FileConfiguration cfg;

    public LandProtectionGriefDefenderConfig() {
        this.file = new File(BASE_DIR, FILE_NAME);
        ensureExistsWithCommentsTemplate();
        reload();
    }

    public void reload() {
        this.cfg = YamlConfiguration.loadConfiguration(file);
        applyMissingDefaultsInMemory();
    }

    public boolean flagsEnabled() {
        return cfg.getBoolean("flags.enabled", true);
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
        try { return EvalOrder.valueOf(s.toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException e) { return EvalOrder.FLAGS_THEN_TRUST; }
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
                        "#   Control how GravesX permissions are evaluated inside GriefDefender (GD) claims.\n" +
                        "#   The addon can consult GD custom flags (namespace is hard-coded to `gravesx`)\n" +
                        "#   and/or fall back to GD trust roles for each action.\n" +
                        "#\n" +
                        "# Evaluation overview:\n" +
                        "#   1) Depending on rules.evaluation-order, evaluate GD flags first or GD trust first.\n" +
                        "#   2) Flags:\n" +
                        "#        - TRUE → allow\n" +
                        "#        - FALSE → deny (can be made to override via rules.flags-deny-overrides)\n" +
                        "#        - UNSET → follow rules.missing-flag-allowed\n" +
                        "#   3) Trust fallback uses per-action levels under `trust.levels.*`.\n" +
                        "#   4) If outside any claim → follow rules.wilderness-allowed.\n" +
                        "#   5) If owner-bypass is enabled and the actor owns the claim → allow.\n" +
                        "#\n" +
                        "# Note:\n" +
                        "#   After editing this file, reload the addon (or restart the server) to apply changes.\n" +
                        "\n" +
                        "flags:\n" +
                        "  # Enable consulting GriefDefender flags (ids like \"gravesx-grave-create\").\n" +
                        "  # If disabled, the addon skips flag checks and goes straight to trust evaluation.\n" +
                        "  enabled: true\n" +
                        "\n" +
                        "rules:\n" +
                        "  # Non-player entities (armor stands, projectiles, mobs) pass checks automatically.\n" +
                        "  # Set to false to enforce checks for non-players too.\n" +
                        "  allow-non-player: true\n" +
                        "\n" +
                        "  # If the Location/world context is invalid (e.g., null or not loaded):\n" +
                        "  #   true → allow (fail-open)\n" +
                        "  #   false → deny (fail-closed)\n" +
                        "  invalid-location-allowed: true\n" +
                        "\n" +
                        "  # If flags are enabled but no explicit flag is found (UNDEFINED):\n" +
                        "  #   true → allow (fail-open)\n" +
                        "  #   false → deny (fail-closed)\n" +
                        "  missing-flag-allowed: true\n" +
                        "\n" +
                        "  # When the Location is not inside any GD claim (wilderness):\n" +
                        "  #   true → allow\n" +
                        "  #   false → deny\n" +
                        "  wilderness-allowed: true\n" +
                        "\n" +
                        "  # If the acting player is the owner of the claim, bypass all checks.\n" +
                        "  owner-bypass: true\n" +
                        "\n" +
                        "  # Order of evaluation:\n" +
                        "  #   FLAGS_THEN_TRUST → try flags first; if UNDEFINED, fall back to trust\n" +
                        "  #   TRUST_THEN_FLAGS → try trust first; if not sufficient, consult flags\n" +
                        "  evaluation-order: FLAGS_THEN_TRUST\n" +
                        "\n" +
                        "  # While reading flags, if any explicit DENY (FALSE) is encountered:\n" +
                        "  #   true → treat as DENY even if another path might ALLOW\n" +
                        "  #   false → do not give DENY precedence (rare)\n" +
                        "  flags-deny-overrides: true\n" +
                        "\n" +
                        "trust:\n" +
                        "  # Per-action fallback trust level used when evaluation reaches GD trust checks.\n" +
                        "  # Valid values:\n" +
                        "  #   ACCESSOR → minimal access (doors/buttons); also satisfied by CONTAINER/BUILDER/MANAGER\n" +
                        "  #   CONTAINER → open containers; also satisfied by BUILDER/MANAGER\n" +
                        "  #   BUILDER → place/break; also satisfied by MANAGER\n" +
                        "  levels:\n" +
                        "    create:     BUILDER     # creating graves is destructive → builder by default\n" +
                        "    teleport:   ACCESSOR    # typically harmless → accessor\n" +
                        "    loot:       CONTAINER   # inventory access → container\n" +
                        "    autoloot:   CONTAINER   # inventory access → container\n" +
                        "    walkover:   ACCESSOR    # benign → accessor\n" +
                        "    projectile: ACCESSOR    # interaction; tighten if desired\n" +
                        "    break:      BUILDER     # destructive → builder\n";

        try (FileWriter fw = new FileWriter(file)) {
            fw.write(template);
        } catch (IOException ignored) {
            // ignored
        }
    }

    private void applyMissingDefaultsInMemory() {
        if (!cfg.isSet("flags.enabled"))
            cfg.set("flags.enabled", true);

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
