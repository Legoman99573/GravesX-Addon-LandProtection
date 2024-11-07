package dev.cwhead.GravesXAddon.integration;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import dev.cwhead.GravesXAddon.LandProtection;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * The {@code WorldGuardImpl} class is responsible for interacting with WorldGuard's region and flag systems
 * to determine if specific actions can be performed on grave-related entities and locations.
 * It handles creating, registering, and checking flags for actions such as grave creation, teleportation,
 * looting, and autolooting, as well as determining region memberships for players.
 */
public class WorldGuardImpl {
    private final LandProtection plugin;
    private final WorldGuard worldGuard;

    /**
     * Constructs a {@code WorldGuardImpl} object.
     *
     * @param plugin the {@code LandProtection} plugin instance
     */
    public WorldGuardImpl(LandProtection plugin) {
        this.plugin = plugin;
        this.worldGuard = WorldGuard.getInstance();
        registerMultipleFlags();
    }

    /**
     * Registers multiple flags for use with WorldGuard regions.
     * Flags include those for controlling grave actions such as autoloot, loot, create, and teleport.
     */
    private void registerMultipleFlags() {
        List<String> flagNames = List.of("gravesx-grave-autoloot", "gravesx-grave-loot", "gravesx-grave-create", "gravesx-grave-teleport");

        for (String flagName : flagNames) {
            registerNewFlag(flagName);
        }
    }

    /**
     * Registers a new state flag with WorldGuard.
     * If a flag with the same name already exists, it returns the existing flag.
     *
     * @param flagName the name of the flag to register
     * @return the registered or existing {@code StateFlag}
     */
    private StateFlag registerNewFlag(String flagName) {
        try {
            StateFlag newFlag = new StateFlag(flagName, true);
            worldGuard.getFlagRegistry().register(newFlag);
            plugin.getLogger().info("Registered Flag " + flagName + " Successfully.");
            return newFlag;
        } catch (FlagConflictException exception) {
            Flag<?> conflictingFlag = worldGuard.getFlagRegistry().get(flagName);
            plugin.getLogger().warning("Failed to Register Flag " + flagName + ". Flag will be ignored.");
            return (conflictingFlag instanceof StateFlag) ? (StateFlag) conflictingFlag : null;
        }
    }

    /**
     * Retrieves a {@code StateFlag} by its name.
     *
     * @param flagName the name of the flag to retrieve
     * @return the {@code StateFlag} associated with the given name
     */
    private StateFlag getFlagName(String flagName) {
        return (StateFlag) worldGuard.getFlagRegistry().get(flagName);
    }

    /**
     * Checks whether the specified entity (player) is allowed to create a grave at the specified location.
     *
     * @param entity the entity to check (must be a player)
     * @param location the location where the grave would be created
     * @return {@code true} if the entity is allowed to create a grave at the location, {@code false} otherwise
     */
    public boolean canCreateGrave(Entity entity, Location location) {
        if (!(entity instanceof Player)) {
            return true;
        }

        StateFlag createFlag = getFlagName("gravesx-grave-create");

        return worldGuard.getPlatform().getRegionContainer().createQuery().testState(
                BukkitAdapter.adapt(location),
                WorldGuardPlugin.inst().wrapPlayer((Player) entity),
                createFlag);
    }

    /**
     * Checks whether the specified entity (player) is allowed to teleport at the specified location.
     *
     * @param entity the entity to check (must be a player)
     * @param location the location where teleportation is being attempted
     * @return {@code true} if the entity is allowed to teleport at the location, {@code false} otherwise
     */
    public boolean canTeleport(Entity entity, Location location) {
        if (!(entity instanceof Player)) {
            return true;
        }

        StateFlag teleportFlag = getFlagName("gravesx-grave-teleport");

        return worldGuard.getPlatform().getRegionContainer().createQuery().testState(
                BukkitAdapter.adapt(location),
                WorldGuardPlugin.inst().wrapPlayer((Player) entity),
                teleportFlag);
    }

    /**
     * Checks whether the specified entity (player) is allowed to loot a grave at the specified location.
     *
     * @param entity the entity to check (must be a player)
     * @param location the location of the grave
     * @return {@code true} if the entity is allowed to loot the grave at the location, {@code false} otherwise
     */
    public boolean canLoot(Entity entity, Location location) {
        if (!(entity instanceof Player)) {
            return true;
        }

        StateFlag lootFlag = getFlagName("gravesx-grave-loot");

        return worldGuard.getPlatform().getRegionContainer().createQuery().testState(
                BukkitAdapter.adapt(location),
                WorldGuardPlugin.inst().wrapPlayer((Player) entity),
                lootFlag);
    }

    /**
     * Checks whether the specified entity (player) is allowed to autoloot a grave at the specified location.
     *
     * @param entity the entity to check (must be a player)
     * @param location the location of the grave
     * @return {@code true} if the entity is allowed to autoloot the grave at the location, {@code false} otherwise
     */
    public boolean canAutoLoot(Entity entity, Location location) {
        if (!(entity instanceof Player)) {
            return true;
        }

        StateFlag autoLootFlag = getFlagName("gravesx-grave-autoloot");

        return worldGuard.getPlatform().getRegionContainer().createQuery().testState(
                BukkitAdapter.adapt(location),
                WorldGuardPlugin.inst().wrapPlayer((Player) entity),
                autoLootFlag);
    }

    /**
     * Checks if the specified player is a member of the given region.
     *
     * @param region the name of the region
     * @param player the player to check
     * @return {@code true} if the player is a member of the region, {@code false} otherwise
     */
    public boolean isMember(String region, Player player) {
        for (RegionManager regionManager : worldGuard.getPlatform().getRegionContainer().getLoaded()) {
            if (regionManager.getRegions().containsKey(region)) {
                ProtectedRegion protectedRegion = regionManager.getRegion(region);

                if (protectedRegion != null) {
                    return protectedRegion.isMember(WorldGuardPlugin.inst().wrapPlayer(player));
                }
            }
        }

        return false;
    }

    /**
     * Retrieves a list of region keys that apply to the specified location.
     *
     * @param location the location to check for applicable regions
     * @return a list of region keys in the format "worldguard|<worldName>|<regionId>"
     */
    public List<String> getRegionKeyList(Location location) {
        List<String> regionNameList = new ArrayList<>();

        if (location.getWorld() != null) {
            RegionManager regionManager = worldGuard.getPlatform().getRegionContainer()
                    .get(BukkitAdapter.adapt(location.getWorld()));

            if (regionManager != null) {
                ApplicableRegionSet applicableRegions = regionManager.getApplicableRegions(BlockVector3
                        .at(location.getBlockX(), location.getBlockY(), location.getBlockZ()));

                for (ProtectedRegion protectedRegion : applicableRegions.getRegions()) {
                    regionNameList.add("worldguard|" + location.getWorld().getName() + "|" + protectedRegion.getId());
                }
            }
        }

        return regionNameList;
    }
}