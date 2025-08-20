package dev.cwhead.GravesXAddon.config;

import dev.cwhead.GravesXAddon.LandProtection;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

public class LandProtectionMessagesConfig {
    public enum Format {
        LEGACY,
        MINI_MESSAGE,
        MINE_DOWN
    }

    private static final String DIR = "plugins/GravesX/Addon/Land-Protection";
    private static final String FILE = "messages.yml";

    private final LandProtection plugin;
    private final File file;
    private FileConfiguration cfg;

    public LandProtectionMessagesConfig(LandProtection plugin) {
        this.plugin = plugin;
        this.file = new File(DIR, FILE);
        ensureDefaults();
        reload();
    }

    public void reload() {
        this.cfg = YamlConfiguration.loadConfiguration(file);
        applyMissingDefaults();
        saveQuietly();
    }

    /** Full deny line for a given action key, using template + action text pool. */
    public String denyMessageForAction(String actionKey, String defaultActionText) {
        String actionText = cfg.getString("messages.actions." + actionKey, defaultActionText);
        String template = cfg.getString("messages.templates.deny", "&7\u2620 &cYou must be a member of the region or have permission to {action}");
        String raw = template.replace("{action}", actionText);
        return toLegacy(raw);
    }

    private String toLegacy(String raw) {
        Format fmt = format();
        var im = plugin.getGravesXAPI().getGravesX().getIntegrationManager();

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

    private void ensureDefaults() {
        File dir = new File(DIR);
        if (!dir.exists()) dir.mkdirs();
        if (!file.exists()) {
            this.cfg = new YamlConfiguration();
            cfg.set("format", "LEGACY");

            cfg.set("messages.templates.deny", "&7\u2620 &cYou must be a member of the region or have permission to {action}");

            cfg.set("messages.actions.create", "create a grave here.");
            cfg.set("messages.actions.teleport", "teleport to your grave in this region.");
            cfg.set("messages.actions.open", "open a grave in this region.");
            cfg.set("messages.actions.auto_loot", "auto loot a grave in this region.");
            cfg.set("messages.actions.walk_over", "walk over a grave in this region.");
            cfg.set("messages.actions.projectile", "use a projectile on a grave in this region.");
            cfg.set("messages.actions.break", "break a grave in this region.");

            saveQuietly();
        }
    }

    private void applyMissingDefaults() {
        if (!cfg.isSet("format")) cfg.set("format", "LEGACY");

        if (!cfg.isSet("messages.templates.deny"))
            cfg.set("messages.templates.deny", "&7\u2620 &cYou must be a member of the region or have permission to {action}");

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

    private void saveQuietly() {
        try { cfg.save(file); } catch (IOException ignored) {}
    }
}
