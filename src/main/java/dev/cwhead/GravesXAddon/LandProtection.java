package dev.cwhead.GravesXAddon;

import com.ranull.graves.Graves;
import dev.cwhead.GravesX.GravesXAPI;
import dev.cwhead.GravesXAddon.integration.WorldGuardImpl;
import dev.cwhead.GravesXAddon.listener.LandProtectionWorldGuardGraveCreateListener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public final class LandProtection extends JavaPlugin {

    private GravesXAPI gravesXAPI;

    private static LandProtection instance;

    private WorldGuardImpl worldGuard;

    @Override
    public void onLoad() {
        try {
           worldGuard = new WorldGuardImpl(this);
        } catch (Exception ignored) {
            // ignored
        }
    }

    @Override
    public void onEnable() {
        Plugin gravesX = getServer().getPluginManager().getPlugin("GravesX");
        if (gravesX != null && gravesX.isEnabled()) {
            gravesXAPI = new GravesXAPI((Graves) gravesX);

            instance = this;

            Plugin worldGuard = getServer().getPluginManager().getPlugin("WorldGuard");

            if (worldGuard != null && worldGuard.isEnabled()) {
                try {
                    getServer().getPluginManager().registerEvents(new LandProtectionWorldGuardGraveCreateListener(this), this);
                    getLogger().info("Hooked into " + worldGuard.getDescription().getName() + " v." + worldGuard.getDescription().getVersion());
                    getLogger().info("Hooked into GravesX. WorldGuard Region handling will be handled by GravesX Addon: Land Protection");
                } catch (Exception e) {
                    getLogger().warning("Failed to hook into " + worldGuard.getDescription().getName() + " v." + worldGuard.getDescription().getVersion() + ". WorldGuard regions will be ignored.");
                    getGravesXAPI().getGravesX().logStackTrace(e);
                }
            }

            getLogger().info("Loaded GravesX Addon: Land Protection");
        } else {
            getLogger().severe("Plugin GravesX is either missing or not enabled. Disabling Plugin.");
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("Land Protection Addon Disabled.");
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
}
