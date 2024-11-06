package dev.cwhead.GravesXAddon;

import com.ranull.graves.Graves;
import dev.cwhead.GravesX.GravesXAPI;
import dev.cwhead.GravesXAddon.integration.WorldGuardImpl;
import dev.cwhead.GravesXAddon.listener.LandProtectionWorldGuardGraveCreateListener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main class for the Land Protection addon for GravesX. This plugin integrates with WorldGuard
 * to handle region-based permissions for grave creation, teleportation, opening, and looting.
 * It also ensures compatibility with GravesX and provides event handling for interactions
 * related to graveyards and WorldGuard regions.
 */
public final class LandProtection extends JavaPlugin {

    private GravesXAPI gravesXAPI;

    private static LandProtection instance;

    private WorldGuardImpl worldGuard;

    private boolean worldGuardEnabled;

    /**
     * Called when the plugin is loading. Tries to initialize the WorldGuard integration.
     * If WorldGuard is not available, it silently ignores the failure.
     */
    @Override
    public void onLoad() {
        try {
           worldGuard = new WorldGuardImpl(this);
           worldGuardEnabled = true;
           getLogger().info("Registered WorldGuard Flags Successfully.");
        } catch (Exception ignored) {
            getLogger().warning("Failed to register WorldGuard Flags. Flags will be ignored.");
            worldGuardEnabled = false;
        }
    }

    /**
     * Called when the plugin is enabled. This method hooks into GravesX and WorldGuard (if available),
     * registers event listeners, and logs relevant information to the console.
     *
     * @throws IllegalStateException If the GravesX plugin is not found or enabled, the plugin will disable itself.
     */
    @Override
    public void onEnable() {
        Plugin gravesX = getServer().getPluginManager().getPlugin("GravesX");
        if (gravesX != null && gravesX.isEnabled()) {
            gravesXAPI = new GravesXAPI((Graves) gravesX);

            instance = this;

            Plugin worldGuard = getServer().getPluginManager().getPlugin("WorldGuard");

            if (worldGuardEnabled && (worldGuard != null && worldGuard.isEnabled())) {
                try {
                    getServer().getPluginManager().registerEvents(new LandProtectionWorldGuardGraveCreateListener(this), this);
                    getLogger().info("Hooked into " + worldGuard.getDescription().getName() + " v." + worldGuard.getDescription().getVersion() + ". WorldGuard Region handling will be handled by GravesX Addon: Land Protection");
                    worldGuardEnabled = true;
                } catch (Exception e) {
                    getLogger().warning("Failed to hook into " + worldGuard.getDescription().getName() + " v." + worldGuard.getDescription().getVersion() + ". WorldGuard regions will be ignored.");
                    getGravesXAPI().getGravesX().logStackTrace(e);
                    worldGuardEnabled = false;
                }
            }

            getLogger().info("Loaded GravesX Addon: Land Protection");
        } else {
            getLogger().severe("Plugin GravesX is either missing or not enabled. Disabling Plugin.");
        }
    }

    /**
     * Called when the plugin is disabled. Logs a message indicating that the Land Protection addon is disabled.
     */
    @Override
    public void onDisable() {
        getLogger().info("Land Protection Addon Disabled.");
    }

    /**
     * Gets the GravesXAPI instance associated with the plugin. This API provides access to GravesX functionality.
     *
     * @return The GravesXAPI instance.
     */
    public GravesXAPI getGravesXAPI() {
        return gravesXAPI;
    }

    /**
     * Gets the singleton instance of the LandProtection plugin.
     *
     * @return The singleton instance of the LandProtection plugin.
     */
    public static LandProtection getInstance() {
        return instance;
    }

    /**
     * Gets the WorldGuardImpl instance, which handles WorldGuard integration for this plugin.
     *
     * @return The WorldGuardImpl instance.
     */
    public WorldGuardImpl getWorldGuard() {
        return worldGuard;
    }
}