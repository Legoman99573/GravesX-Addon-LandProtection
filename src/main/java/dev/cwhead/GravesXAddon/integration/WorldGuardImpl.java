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

public class WorldGuardImpl {
    private final LandProtection plugin;
    private final WorldGuard worldGuard;

    public WorldGuardImpl(LandProtection plugin) {
        this.plugin = plugin;
        this.worldGuard = WorldGuard.getInstance();
        registerMultipleFlags();
    }

    private void registerMultipleFlags() {
        // List of flag names you want to register
        List<String> flagNames = List.of("gravesx-grave-autoloot", "gravesx-grave-loot", "gravesx-grave-create", "gravesx-grave-teleport");

        // Iterate through the list and register each flag
        for (String flagName : flagNames) {
            registerNewFlag(flagName);
        }
    }

    private StateFlag registerNewFlag(String flagName) {
        try {
            StateFlag newFlag = new StateFlag(flagName, false);
            worldGuard.getFlagRegistry().register(newFlag);
            return newFlag;
        } catch (FlagConflictException exception) {
            Flag<?> conflictingFlag = worldGuard.getFlagRegistry().get(flagName);
            return (conflictingFlag instanceof StateFlag) ? (StateFlag) conflictingFlag : null;
        }
    }

    private StateFlag getFlagName(String flagName) {
        return (StateFlag) worldGuard.getFlagRegistry().get(flagName);
    }

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

    public boolean canTeleport(Entity entity, Location location) {
        if (!(entity instanceof Player)) {
            return true;
        }

        StateFlag createFlag = getFlagName("gravesx-grave-teleport");

        return worldGuard.getPlatform().getRegionContainer().createQuery().testState(
                BukkitAdapter.adapt(location),
                WorldGuardPlugin.inst().wrapPlayer((Player) entity),
                createFlag);
    }

    public boolean canLoot(Entity entity, Location location) {
        if (!(entity instanceof Player)) {
            return true;
        }

        StateFlag createFlag = getFlagName("gravesx-grave-loot");

        return worldGuard.getPlatform().getRegionContainer().createQuery().testState(
                BukkitAdapter.adapt(location),
                WorldGuardPlugin.inst().wrapPlayer((Player) entity),
                createFlag);
    }

    public boolean canAutoLoot(Entity entity, Location location) {
        if (!(entity instanceof Player)) {
            return true;
        }

        StateFlag createFlag = getFlagName("gravesx-grave-autoloot");

        return worldGuard.getPlatform().getRegionContainer().createQuery().testState(
                BukkitAdapter.adapt(location),
                WorldGuardPlugin.inst().wrapPlayer((Player) entity),
                createFlag);
    }

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

    public List<String> getRegions() {
        List<String> regionNames = new ArrayList<>();
        for (RegionManager regionManager : worldGuard.getPlatform().getRegionContainer().getLoaded()) {
            regionNames.addAll(regionManager.getRegions().keySet());
        }
        return regionNames;
    }
}
