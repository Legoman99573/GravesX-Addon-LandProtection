package dev.cwhead.GravesXAddon.integration;

import dev.cwhead.GravesXAddon.LandProtection;
import me.angeschossen.lands.api.LandsIntegration;
import me.angeschossen.lands.api.flags.enums.FlagTarget;
import me.angeschossen.lands.api.flags.enums.RoleFlagCategory;
import me.angeschossen.lands.api.flags.type.RoleFlag;
import me.angeschossen.lands.api.land.Area;
import me.angeschossen.lands.api.land.Land;
import me.angeschossen.lands.api.land.LandWorld;
import me.angeschossen.lands.api.player.LandPlayer;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * The {@code LandsImpl} class is responsible for integrating
 * the GravesX Addon with the Lands (paid) plugin.
 * <p>
 * It registers and manages custom role flags for grave-related actions
 * such as creation, looting, teleportation, and interaction.
 * These flags are used to determine whether players may perform
 * certain actions at locations protected by Lands.
 */
public class LandsImpl {

    private final LandsIntegration api;

    private static RoleFlag FLAG_GRAVE_AUTOLOOT;
    private static RoleFlag FLAG_GRAVE_LOOT;
    private static RoleFlag FLAG_GRAVE_CREATE;
    private static RoleFlag FLAG_GRAVE_TELEPORT;
    private static RoleFlag FLAG_GRAVE_WALKOVER;
    private static RoleFlag FLAG_GRAVE_PROJECTILE;

    /**
     * Constructs a new {@code LandsImpl} instance.
     *
     * @param plugin the {@link LandProtection} plugin instance
     */
    public LandsImpl(LandProtection plugin) {
        this.api = LandsIntegration.of(plugin);
        registerFlagsOnLoad(plugin);
    }

    /**
     * Registers custom GravesX-related flags with the Lands plugin.
     * <p>
     * This should be called from plugin {@code onLoad()} or early in lifecycle
     * to ensure flags are available before Lands fully enables.
     *
     * @param plugin the {@link LandProtection} plugin instance
     */
    public static void registerFlagsOnLoad(LandProtection plugin) {
        LandsIntegration.of(plugin).onLoad(() -> {
            LandsIntegration api = LandsIntegration.of(plugin);

            java.util.function.Function<String, RoleFlag> make = (name) ->
                    RoleFlag.of(api, FlagTarget.PLAYER, RoleFlagCategory.ACTION, name)
                            .setDisplayName(pretty(name))
                            .setDescription(List.of("GravesX Addon control for " + name.replace("gravesx-grave-", "")));

            FLAG_GRAVE_AUTOLOOT   = make.apply("gravesx-grave-autoloot");
            FLAG_GRAVE_LOOT       = make.apply("gravesx-grave-loot");
            FLAG_GRAVE_CREATE     = make.apply("gravesx-grave-create");
            FLAG_GRAVE_TELEPORT   = make.apply("gravesx-grave-teleport");
            FLAG_GRAVE_WALKOVER   = make.apply("gravesx-grave-walkover");
            FLAG_GRAVE_PROJECTILE = make.apply("gravesx-grave-projectile");
        });
    }

    /**
     * Converts a raw flag key into a prettified display string.
     *
     * @param key the raw key (e.g. "gravesx-grave-autoloot")
     * @return prettified display string (e.g. "Gravesx grave autoloot")
     */
    private static String pretty(String key) {
        String s = key.replace('-', ' ');
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    /**
     * Checks if the specified entity is allowed to create a grave at the given location.
     *
     * @param entity the entity to check (should be a {@link Player})
     * @param loc    the location of the grave creation
     * @return {@code true} if the action is allowed, otherwise {@code false}
     */
    public boolean canCreateGrave(Entity entity, Location loc) {
        return testFlag(entity, loc, FLAG_GRAVE_CREATE);
    }

    /**
     * Checks if the specified entity is allowed to teleport at the given location.
     *
     * @param entity the entity to check (should be a {@link Player})
     * @param loc    the location of the teleport
     * @return {@code true} if the action is allowed, otherwise {@code false}
     */
    public boolean canTeleport(Entity entity, Location loc) {
        return testFlag(entity, loc, FLAG_GRAVE_TELEPORT);
    }

    /**
     * Checks if the specified entity is allowed to loot a grave at the given location.
     *
     * @param entity the entity to check (should be a {@link Player})
     * @param loc    the location of the grave
     * @return {@code true} if the action is allowed, otherwise {@code false}
     */
    public boolean canLoot(Entity entity, Location loc) {
        return testFlag(entity, loc, FLAG_GRAVE_LOOT);
    }

    /**
     * Checks if the specified entity is allowed to autoloot a grave at the given location.
     *
     * @param entity the entity to check (should be a {@link Player})
     * @param loc    the location of the grave
     * @return {@code true} if the action is allowed, otherwise {@code false}
     */
    public boolean canAutoLoot(Entity entity, Location loc) {
        return testFlag(entity, loc, FLAG_GRAVE_AUTOLOOT);
    }

    /**
     * Checks if the specified entity is allowed to walk over a grave at the given location.
     *
     * @param entity the entity to check (should be a {@link Player})
     * @param loc    the location of the grave
     * @return {@code true} if the action is allowed, otherwise {@code false}
     */
    public boolean canWalkOver(Entity entity, Location loc) {
        return testFlag(entity, loc, FLAG_GRAVE_WALKOVER);
    }

    /**
     * Checks if the specified entity is allowed to use projectiles on a grave at the given location.
     *
     * @param entity the entity to check (should be a {@link Player})
     * @param loc    the location of the grave
     * @return {@code true} if the action is allowed, otherwise {@code false}
     */
    public boolean canProjectile(Entity entity, Location loc) {
        return testFlag(entity, loc, FLAG_GRAVE_PROJECTILE);
    }

    /**
     * Determines whether the given player is a trusted member of the land region
     * identified by the provided region key.
     *
     * @param regionKey the region key in the format "lands|world|landName"
     * @param player    the player to check
     * @return {@code true} if the player is trusted in the land, otherwise {@code false}
     */
    public boolean isMember(String regionKey, Player player) {
        if (api == null || player == null || regionKey == null || !regionKey.toLowerCase(Locale.ROOT).startsWith("lands|"))
            return false;

        String[] parts = regionKey.split("\\|");
        if (parts.length < 3) return false;

        String landName = parts[2];
        Land land = api.getLandByName(landName);
        if (land == null) return false;

        UUID uid = player.getUniqueId();
        return land.isTrusted(uid);
    }

    /**
     * Retrieves a list of region keys that apply to the specified location.
     * <p>
     * Keys are in the format:
     * <ul>
     *     <li>"lands|world|landName"</li>
     *     <li>"lands|world|landName|areaName" (if within a sub-area)</li>
     * </ul>
     *
     * @param location the location to check
     * @return list of applicable region keys
     */
    public List<String> getRegionKeyList(Location location) {
        List<String> keys = new ArrayList<>();
        if (api == null || location == null || location.getWorld() == null) return keys;

        World bw = location.getWorld();
        LandWorld lw = api.getWorld(bw);
        if (lw == null) return keys;

        Area area = lw.getArea(location.getBlockX(), location.getBlockY(), location.getBlockZ());
        if (area != null) {
            Land land = area.getLand();
            String base = "lands|" + bw.getName() + "|" + land.getName();
            keys.add(base);
            keys.add(base + "|" + area.getName());
            return keys;
        }

        Land land = api.getLandByChunk(bw, location.getBlockX() >> 4, location.getBlockZ() >> 4);
        if (land != null) {
            keys.add("lands|" + bw.getName() + "|" + land.getName());
        }
        return keys;
    }

    /**
     * Internal helper that tests whether the given entity has permission for the specified flag
     * at the given location.
     *
     * @param entity   the entity to check
     * @param location the location of the action
     * @param flag     the role flag to test
     * @return {@code true} if the entity may perform the action, otherwise {@code false}
     */
    private boolean testFlag(Entity entity, Location location, RoleFlag flag) {
        if (!(entity instanceof Player)) return true;
        if (api == null || flag == null) return true;
        if (location == null || location.getWorld() == null) return true;

        LandWorld lw = api.getWorld(location.getWorld());
        if (lw == null) return true;

        LandPlayer lp = api.getLandPlayer((entity).getUniqueId());
        if (lp == null) return true;

        return lw.hasRoleFlag(lp, location, flag, null, false);
    }
}
