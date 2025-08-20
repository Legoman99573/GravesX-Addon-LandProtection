package dev.cwhead.GravesXAddon.integration;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import dev.cwhead.GravesXAddon.LandProtection;
import dev.cwhead.GravesXAddon.config.LandProtectionWorldGuardConfig;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * WorldGuard integration: registers GravesX flags and evaluates region permissions/membership.
 */
public class WorldGuardImpl {

    private final LandProtection plugin;
    private final WorldGuard wg;
    private final FlagRegistry registry;
    private final LandProtectionWorldGuardConfig config;

    private final Map<String, StateFlag> registered = new LinkedHashMap<>();

    public static final String F_AUTOLOOT = "gravesx-grave-autoloot";
    public static final String F_LOOT = "gravesx-grave-loot";
    public static final String F_CREATE = "gravesx-grave-create";
    public static final String F_TELEPORT = "gravesx-grave-teleport";
    public static final String F_WALKOVER = "gravesx-grave-walkover";
    public static final String F_PROJECTILE = "gravesx-grave-projectile";
    public static final String F_BREAK = "gravesx-grave-break";

    public WorldGuardImpl(LandProtection plugin) {
        this.plugin = plugin;
        this.wg = WorldGuard.getInstance();
        this.registry = wg.getFlagRegistry();
        this.config = new LandProtectionWorldGuardConfig();
        registerConfiguredFlags();
    }

    public void reloadConfig() {
        config.reload();
        registered.clear();
        registerConfiguredFlags();
    }

    private void registerConfiguredFlags() {
        if (!config.shouldRegisterFlags()) {
            plugin.getLogger().info("WorldGuard flag registration disabled by config.");
            for (String id : defaultFlagOrder()) {
                StateFlag f = getExistingFlag(id);
                if (f != null) registered.put(id, f);
            }
            return;
        }

        boolean globalDefault = config.defaultFlagState();
        Map<String, Boolean> perFlag = config.flagDefaults();

        for (String id : defaultFlagOrder()) {
            boolean def = perFlag.getOrDefault(id, globalDefault);
            StateFlag f = registerOrGetFlag(id, def);
            if (f != null) registered.put(id, f);
        }
    }

    private List<String> defaultFlagOrder() {
        return Arrays.asList(
                F_AUTOLOOT,
                F_LOOT,
                F_CREATE,
                F_TELEPORT,
                F_WALKOVER,
                F_PROJECTILE,
                F_BREAK
        );
    }

    private StateFlag registerOrGetFlag(String name, boolean defaultState) {
        try {
            StateFlag f = new StateFlag(name, defaultState);
            registry.register(f);
            plugin.getLogger().info("Registered flag: " + name + " (default=" + defaultState + ")");
            return f;
        } catch (FlagConflictException e) {
            Flag<?> existing = registry.get(name);
            if (existing instanceof StateFlag) {
                plugin.getLogger().info("Flag already exists, using existing: " + name);
                return (StateFlag) existing;
            }
            plugin.getLogger().warning("Flag name conflict (non-StateFlag): " + name + ". Flag will be ignored.");
            return null;
        }
    }

    private StateFlag getExistingFlag(String name) {
        Flag<?> f = registry.get(name);
        return (f instanceof StateFlag) ? (StateFlag) f : null;
    }

    public boolean canCreateGrave(Entity entity, Location loc) {
        return check(entity, loc, registered.get(F_CREATE));
    }

    public boolean canTeleport(Entity entity, Location loc) {
        return check(entity, loc, registered.get(F_TELEPORT));
    }

    public boolean canLoot(Entity entity, Location loc) {
        return check(entity, loc, registered.get(F_LOOT));
    }

    public boolean canAutoLoot(Entity entity, Location loc) {
        return check(entity, loc, registered.get(F_AUTOLOOT));
    }

    public boolean canWalkOver(Entity entity, Location loc) {
        return check(entity, loc, registered.get(F_WALKOVER));
    }

    public boolean canProjectile(Entity entity, Location loc) {
        return check(entity, loc, registered.get(F_PROJECTILE));
    }

    public boolean canBreak(Entity entity, Location loc) {
        return check(entity, loc, registered.get(F_BREAK));
    }

    private boolean check(Entity entity, Location loc, StateFlag flag) {
        if (!(entity instanceof Player)) {
            return config.allowNonPlayer();
        }
        if (loc == null || loc.getWorld() == null) {
            return config.invalidLocationAllowed();
        }
        if (flag == null) {
            return config.missingFlagAllowed();
        }

        return wg.getPlatform()
                .getRegionContainer()
                .createQuery()
                .testState(
                        BukkitAdapter.adapt(loc),
                        WorldGuardPlugin.inst().wrapPlayer((Player) entity),
                        flag
                );
    }

    public boolean isMember(String regionId, Player player) {
        if (player == null) return false;

        for (RegionManager mgr : wg.getPlatform().getRegionContainer().getLoaded()) {
            ProtectedRegion pr = mgr.getRegion(regionId);
            if (pr != null && pr.isMember(WorldGuardPlugin.inst().wrapPlayer(player))) {
                return true;
            }
        }
        return false;
    }

    public List<String> getRegionKeyList(Location loc) {
        List<String> out = new ArrayList<>();
        if (loc == null || loc.getWorld() == null) return out;

        RegionManager mgr = wg.getPlatform().getRegionContainer().get(BukkitAdapter.adapt(loc.getWorld()));
        if (mgr == null) return out;

        ApplicableRegionSet ars = mgr.getApplicableRegions(BlockVector3.at(
                loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()
        ));
        for (ProtectedRegion pr : ars.getRegions()) {
            out.add("worldguard|" + loc.getWorld().getName() + "|" + pr.getId());
        }
        return out;
    }
}