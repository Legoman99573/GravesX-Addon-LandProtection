package dev.cwhead.GravesXAddon;

import com.ranull.graves.Graves;
import dev.cwhead.GravesX.GravesXAPI;
import dev.cwhead.GravesXAddon.commands.LandProtectionCommand;
import dev.cwhead.GravesXAddon.config.LandProtectionMainConfig;
import dev.cwhead.GravesXAddon.integration.GriefDefenderImpl;
import dev.cwhead.GravesXAddon.integration.GriefPreventionImpl;
import dev.cwhead.GravesXAddon.integration.LandsImpl;
import dev.cwhead.GravesXAddon.integration.TownyImpl;
import dev.cwhead.GravesXAddon.integration.WorldGuardImpl;
import dev.cwhead.GravesXAddon.listener.LandProtectionListener;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.function.BiConsumer;

/**
 * Main class for the Land Protection addon for GravesX.
 */
public final class LandProtection extends JavaPlugin {

    private GravesXAPI gravesXAPI;
    private static LandProtection instance;
    private LandProtectionMainConfig mainConfig;

    private WorldGuardImpl worldGuard;
    private TownyImpl towny;
    private LandsImpl lands;
    private GriefDefenderImpl griefDefender;
    private GriefPreventionImpl griefPrevention;

    private boolean worldGuardEnabled = false;
    private boolean townyEnabled = false;
    private boolean landsEnabled = false;
    private boolean griefDefenderEnabled = false;
    private boolean griefPreventionEnabled = false;

    @Override
    public void onLoad() {
        try {
            final Plugin wg = getServer().getPluginManager().getPlugin("WorldGuard");
            if (wg == null) {
                return;
            }
            worldGuard = new WorldGuardImpl(this);
            worldGuardEnabled = true;
        } catch (Exception ignored) {
            //ignored
        }
    }


    @Override
    public void onEnable() {
        final Plugin gravesX = getServer().getPluginManager().getPlugin("GravesX");
        if (gravesX == null || !gravesX.isEnabled()) {
            getLogger().severe("Plugin GravesX is either missing or not enabled. Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.gravesXAPI = new GravesXAPI((Graves) gravesX);
        instance = this;

        if (worldGuardEnabled) {
            final Plugin wg = getServer().getPluginManager().getPlugin("WorldGuard");
            if (wg != null && wg.isEnabled()) {
                try {
                    getServer().getPluginManager().registerEvents(new LandProtectionListener(this), this);
                    logHookSuccess(wg);
                } catch (Exception e) {
                    logHookFailure(wg, e, "WorldGuard regions will be ignored.");
                    worldGuardEnabled = false;
                }
            } else {
                worldGuardEnabled = false;
            }
        }

        townyEnabled = hook("Towny", (pl, lp) -> lp.towny = new TownyImpl(lp));
        landsEnabled = hook("Lands", (pl, lp) -> lp.lands = new LandsImpl(lp));
        griefDefenderEnabled = hook("GriefDefender", (pl, lp) -> lp.griefDefender = new GriefDefenderImpl(lp));
        griefPreventionEnabled = hook("GriefPrevention", (pl, lp) -> lp.griefPrevention = new GriefPreventionImpl(lp));

        if (!worldGuardEnabled && !townyEnabled && !landsEnabled && !griefDefenderEnabled && !griefPreventionEnabled) {
            getLogger().warning("Failed to hook into any supported Land Protection plugin. Disabling plugin...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        registerMetrics();

        ensureAddonDirs();

        LandProtectionCommand cmd = new LandProtectionCommand(this);
        if (getCommand("gxlp") != null) {
            getCommand("gxlp").setExecutor(cmd);
            getCommand("gxlp").setTabCompleter(cmd);
        }

        getLogger().info("Loaded GravesX Addon: Land Protection");
    }

    private void registerMetrics() {
        if (getMainConfig().metricsEnabled()) {
            Metrics metrics = new Metrics(this, 120633);

            metrics.addCustomChart(new SimplePie("griefdefender", () -> String.valueOf(isGriefDefenderEnabled()).toLowerCase()));
            metrics.addCustomChart(new SimplePie("griefprevention", () -> String.valueOf(isGriefPreventionEnabled()).toLowerCase()));
            metrics.addCustomChart(new SimplePie("towny", () -> String.valueOf(isTownyEnabled()).toLowerCase()));
            metrics.addCustomChart(new SimplePie("lands", () -> String.valueOf(isLandsEnabled()).toLowerCase()));
            metrics.addCustomChart(new SimplePie("worldguard", () -> String.valueOf(isWorldGuardEnabled()).toLowerCase()));
        }
    }

    private void ensureAddonDirs() {
        try {
            java.io.File pluginsDir = getDataFolder().getParentFile();
            java.io.File gravesXDir = new java.io.File(pluginsDir, "GravesX");

            java.io.File addonDir = new java.io.File(gravesXDir, "addon");
            if (!addonDir.exists()) addonDir.mkdir();

            java.io.File lpDir = new java.io.File(addonDir, "Land-Protection");
            if (!lpDir.exists()) lpDir.mkdir();
        } catch (Exception e) {
            getLogger().severe("An issue occured while generating /plugins/GravesX/addon/Land-Protection. Cause: " + e.getCause());
            getGravesXAPI().getGravesX().logStackTrace(e);
            getLogger().severe("Disabling plugin...");
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("Land Protection Addon Disabled.");
    }

    private boolean hook(String pluginName, BiConsumer<Plugin, LandProtection> initializer) {
        final Plugin target = getServer().getPluginManager().getPlugin(pluginName);
        if (target == null || !target.isEnabled()) return false;

        try {
            initializer.accept(target, this);
            logHookSuccess(target);
            return true;
        } catch (Exception e) {
            logHookFailure(target, e, pluginName + " handling will be ignored.");
            return false;
        }
    }

    private void logHookSuccess(Plugin pl) {
        getLogger().info("Hooked into " + pl.getDescription().getName() +
                " v" + pl.getDescription().getVersion() + ".");
    }

    private void logHookFailure(Plugin pl, Exception e, String extra) {
        getLogger().warning("Failed to hook into " + pl.getDescription().getName() +
                " v" + pl.getDescription().getVersion() + ". " + extra);
        try {
            if (getGravesXAPI() != null && getGravesXAPI().getGravesX() != null) {
                getGravesXAPI().getGravesX().logStackTrace(e);
            }
        } catch (Throwable ignored) {
            // ignored
        }
    }


    public GravesXAPI getGravesXAPI() {
        return gravesXAPI;
    }

    public static LandProtection getInstance() {
        return instance;
    }

    public WorldGuardImpl getWorldGuard() {
        return worldGuard;
    }

    public TownyImpl getTowny() {
        return towny;
    }

    public LandsImpl getLands() {
        return lands;
    }

    public GriefDefenderImpl getGriefDefender() {
        return griefDefender;
    }

    public GriefPreventionImpl getGriefPrevention() {
        return griefPrevention;
    }

    public boolean isWorldGuardEnabled() {
        return worldGuardEnabled;
    }

    public boolean isTownyEnabled() {
        return townyEnabled;
    }

    public boolean isLandsEnabled() {
        return landsEnabled;
    }

    public boolean isGriefDefenderEnabled() {
        return griefDefenderEnabled;
    }

    public boolean isGriefPreventionEnabled() {
        return griefPreventionEnabled;
    }

    public LandProtectionMainConfig getMainConfig() {
        return mainConfig;
    }

    public void reloadAllConfigs() {
        try {
            if (worldGuard != null)
                worldGuard.reloadConfig();
        } catch (Throwable ignored) {
            //ignored
        }

        try {
            if (towny != null)
                towny.reloadConfig();
        } catch (Throwable ignored) {
            //ignored
        }

        try {
            if (lands != null)
                lands.reloadConfig();
        } catch (Throwable ignored) {
            //ignored
        }
        try {
            if (griefDefender != null)
                griefDefender.reloadConfig();
        } catch (Throwable ignored) {
            //ignored
        }

        try {
            if (griefPrevention != null)
                griefPrevention.reloadConfig();
        } catch (Throwable ignored) {
            //ignored
        }
    }
}