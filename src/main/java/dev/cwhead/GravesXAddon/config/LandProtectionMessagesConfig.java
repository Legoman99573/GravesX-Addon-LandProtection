package dev.cwhead.GravesXAddon.config;

import dev.cwhead.GravesXAddon.LandProtection;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Locale;

public class LandProtectionMessagesConfig {
    public enum Format {
        LEGACY,
        MINI_MESSAGE,
        MINE_DOWN
    }

    private static final String DIR = "plugins/GravesX/addon/Land-Protection";
    private static final String FILE = "messages.yml";

    private final LandProtection plugin;
    private final File file;
    private FileConfiguration cfg;

    public LandProtectionMessagesConfig(LandProtection plugin) {
        this.plugin = plugin;
        this.file = new File(DIR, FILE);
        ensureExistsWithCommentsTemplate();
        reload();
    }

    public void reload() {
        this.cfg = YamlConfiguration.loadConfiguration(file);
        applyMissingDefaultsInMemory();
    }

    public String denyMessageForAction(String actionKey, String defaultActionText) {
        String actionText = cfg.getString("messages.actions." + actionKey, defaultActionText);
        String template = cfg.getString("messages.deny", "&7\u2620 &cYou must be a member of the region or have permission to {action}");
        String raw = template.replace("{action}", actionText);
        return toLegacy(raw);
    }

    private String toLegacy(String raw) {
        Format fmt = format();
        var im = plugin.getGravesXAPI().plugin().getIntegrationManager();

        try {
            if (fmt == Format.MINI_MESSAGE && im.hasMiniMessage()) {
                return com.ranull.graves.integration.MiniMessage.parseString(raw);
            }
            if (fmt == Format.MINE_DOWN && im.hasMineDown()) {
                return new com.ranull.graves.integration.MineDown().parseString(raw);
            }
            return ChatColor.translateAlternateColorCodes('&', raw);
        } catch (Throwable ignored) {
            return ChatColor.translateAlternateColorCodes('&', raw);
        }
    }

    private Format format() {
        String s = cfg.getString("format", "LEGACY");
        try { return Format.valueOf(s.toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException e) { return Format.LEGACY; }
    }

    private void ensureExistsWithCommentsTemplate() {
        File dir = new File(DIR);
        if (!dir.exists()) dir.mkdirs();
        if (file.exists()) return;

        String template =
                "# Formatting mode for all message strings below.\n" +
                        "#   - LEGACY: Ampersand color codes (&7, &c, &l, etc.). Safest default.\n" +
                        "#   - MINI_MESSAGE: MiniMessage tags (<gray>, <red>, <bold>, etc.). Will be converted to legacy if supported.\n" +
                        "#   - MINE_DOWN: MineDown syntax (also accepts legacy codes). Will be converted to legacy if supported.\n" +
                        "# If the chosen format’s integration isn’t available at runtime, messages fall back to LEGACY parsing.\n" +
                        "format: LEGACY\n" +
                        "\n" +
                        "messages:\n" +
                        "  # Denial message template used whenever a player lacks permission/membership.\n" +
                        "  # Placeholders:\n" +
                        "  #   {action} — replaced with the human-readable action text from messages.actions.<key> below.\n" +
                        "  # For LEGACY mode, use '&' color codes. For MINI_MESSAGE or MINE_DOWN, write in that syntax.\n" +
                        "  deny: \"&7☠ &cYou must be a member of the region or have permission to {action}\"\n" +
                        "\n" +
                        "  # Human-readable phrases for each action. These are inserted into {action} in the template above.\n" +
                        "  # Edit freely without changing code.\n" +
                        "  actions:\n" +
                        "    # Shown when a player tries to create a grave where they’re not allowed.\n" +
                        "    create: \"create a grave here.\"\n" +
                        "    # Shown when teleporting to a grave is blocked by a protection system.\n" +
                        "    teleport: \"teleport to your grave in this region.\"\n" +
                        "    # Shown when opening a grave is not permitted.\n" +
                        "    open: \"open a grave in this region.\"\n" +
                        "    # Shown when auto-looting a grave is not permitted.\n" +
                        "    auto_loot: \"auto loot a grave in this region.\"\n" +
                        "    # Shown when simply walking over a grave is blocked (rare, but configurable).\n" +
                        "    walk_over: \"walk over a grave in this region.\"\n" +
                        "    # Shown when using a projectile on a grave is blocked (e.g., trying to destroy/interact via projectile).\n" +
                        "    projectile: \"use a projectile on a grave in this region.\"\n" +
                        "    # Shown when breaking a grave is not permitted.\n" +
                        "    break: \"break a grave in this region.\"\n";

        try (FileWriter fw = new FileWriter(file)) {
            fw.write(template);
        } catch (IOException ignored) {
            // ignored
        }
    }

    private void applyMissingDefaultsInMemory() {
        if (!cfg.isSet("format")) cfg.set("format", "LEGACY");

        if (!cfg.isSet("messages.deny"))
            cfg.set("messages.deny", "&7\u2620 &cYou must be a member of the region or have permission to {action}");

        ensureActionDefault("create", "create a grave here.");
        ensureActionDefault("teleport", "teleport to your grave in this region.");
        ensureActionDefault("open", "open a grave in this region.");
        ensureActionDefault("auto_loot", "auto loot a grave in this region.");
        ensureActionDefault("walk_over", "walk over a grave in this region.");
        ensureActionDefault("projectile", "use a projectile on a grave in this region.");
        ensureActionDefault("break", "break a grave in this region.");
    }

    private void ensureActionDefault(String key, String value) {
        String path = "messages.actions." + key;
        if (!cfg.isSet(path)) cfg.set(path, value);
    }

    @SuppressWarnings("unused")
    private void saveQuietly() {
        try { cfg.save(file); } catch (IOException ignored) {}
    }
}