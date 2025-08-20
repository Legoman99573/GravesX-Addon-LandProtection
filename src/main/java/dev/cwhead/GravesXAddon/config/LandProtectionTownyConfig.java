package dev.cwhead.GravesXAddon.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class LandProtectionTownyConfig {

    private static final String BASE_DIR = "plugins/GravesX/addon/Land-Protection";
    private static final String FILE_NAME = "towny.yml";

    private final File file;
    private FileConfiguration cfg;

    public LandProtectionTownyConfig() {
        this.file = new File(BASE_DIR, FILE_NAME);
        ensureExistsWithCommentsTemplate();
        reload();
    }

    public void reload() {
        this.cfg = YamlConfiguration.loadConfiguration(file);
        applyMissingDefaultsInMemory();
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

    private void ensureExistsWithCommentsTemplate() {
        File dir = new File(BASE_DIR);
        if (!dir.exists()) dir.mkdirs();
        if (file.exists()) return;

        String template =
                "# Purpose:\n" +
                        "#   Decide which Towny plot types (TownBlockType enum IDs) grant permission to\n" +
                        "#   players who are allies or total outsiders when interacting with GravesX features.\n" +
                        "#\n" +
                        "# How it’s evaluated (typical flow in the addon):\n" +
                        "#   1) If rules.residents-always-allowed = true and the player is a Resident of the plot’s town → ALLOW.\n" +
                        "#   2) Else, if the player is considered an “ally” (per Towny relations):\n" +
                        "#        - If the plot’s type is in ally-allowed-types → ALLOW.\n" +
                        "#   3) Else (player is an outsider / neither resident nor ally):\n" +
                        "#        - If the plot’s type is in outsider-allowed-types → ALLOW.\n" +
                        "#   4) Else, no allowance is granted by Towny (other integrations may still allow/deny).\n" +
                        "#\n" +
                        "# Notes:\n" +
                        "#   • Use EXACT Towny TownBlockType enum IDs (NOT display names). Common examples include:\n" +
                        "#       RESIDENTIAL, COMMERCIAL, EMBASSY, ARENA, FARM, INN, JAIL, WILDERNESS (do NOT use WILDERNESS here; see rules.wilderness-allowed)\n" +
                        "#     Refer to your Towny version for the definitive list.\n" +
                        "#   • If a list is empty, that group (ally/outsider) receives NO special allowance from Towny by plot type.\n" +
                        "#   • These lists apply uniformly to all GravesX actions; fine-grained per-action rules (if desired) should be\n" +
                        "#     implemented in your code/config beyond this file.\n" +
                        "#\n" +
                        "# Allies get access on these plot types.\n" +
                        "ally-allowed-types: [EMBASSY, ARENA]\n" +
                        "\n" +
                        "# Outsiders (neither residents nor allies) get access on these plot types.\n" +
                        "outsider-allowed-types: []\n" +
                        "\n" +
                        "rules:\n" +
                        "  # If the location is not inside any Town (wilderness):\n" +
                        "  #   true → allow (Towny imposes no restriction in wilderness)\n" +
                        "  #   false → deny by Towny in wilderness\n" +
                        "  wilderness-allowed: true\n" +
                        "\n" +
                        "  # If the acting player is a Resident of the town that owns the plot:\n" +
                        "  #   true → always allow (bypass ally/outsider lists)\n" +
                        "  #   false → require the plot type to be listed above (ally/outsider) to allow\n" +
                        "  residents-always-allowed: true\n";

        try (FileWriter fw = new FileWriter(file)) {
            fw.write(template);
        } catch (IOException ignored) {
            // ignored
        }
    }

    private void applyMissingDefaultsInMemory() {
        if (!cfg.isSet("ally-allowed-types"))
            cfg.set("ally-allowed-types", Arrays.asList("EMBASSY", "ARENA"));

        if (!cfg.isSet("outsider-allowed-types"))
            cfg.set("outsider-allowed-types", Collections.emptyList());

        if (!cfg.isSet("rules.wilderness-allowed"))
            cfg.set("rules.wilderness-allowed", true);

        if (!cfg.isSet("rules.residents-always-allowed"))
            cfg.set("rules.residents-always-allowed", true);
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
