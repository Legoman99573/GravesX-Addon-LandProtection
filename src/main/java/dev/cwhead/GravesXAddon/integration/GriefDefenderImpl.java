package dev.cwhead.GravesXAddon.integration;

import dev.cwhead.GravesXAddon.LandProtection;
import com.griefdefender.api.GriefDefender;
import com.griefdefender.api.Subject;
import com.griefdefender.api.Tristate;
import com.griefdefender.api.claim.Claim;
import com.griefdefender.api.claim.TrustTypes;
import com.griefdefender.api.permission.Context;
import com.griefdefender.api.permission.PermissionManager;
import com.griefdefender.api.permission.flag.Flag;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Lands (paid) integration.
 * <p>
 * It registers and manages custom role flags for grave-related actions
 * such as creation, looting, teleportation, and interaction.
 * These flags are used to determine whether players may perform
 * certain actions at locations protected by Lands.
 */
public class GriefDefenderImpl {

    private static final String FLAG_CREATE     = "gravesx-grave-create";
    private static final String FLAG_TELEPORT   = "gravesx-grave-teleport";
    private static final String FLAG_LOOT       = "gravesx-grave-loot";
    private static final String FLAG_AUTOLOOT   = "gravesx-grave-autoloot";
    private static final String FLAG_WALKOVER   = "gravesx-grave-walkover";
    private static final String FLAG_PROJECTILE = "gravesx-grave-projectile";
    private static final String FLAG_BREAK      = "gravesx-grave-break";

    private final LandProtection plugin;

    /**
     * Constructs a new GriefDefender integration handler.
     *
     * @param plugin the LandProtection plugin instance
     */
    public GriefDefenderImpl(LandProtection plugin) {
        this.plugin = plugin;
    }

    /**
     * Checks if a grave can be created at the given location.
     *
     * @param entity the acting entity
     * @param loc    the target location
     * @return true if allowed
     */
    public boolean canCreateGrave(Entity entity, Location loc) {
        return check(entity, loc, RequiredTrust.BUILDER, FLAG_CREATE);
    }

    /**
     * Checks if teleporting to a grave is allowed at the given location.
     *
     * @param entity the acting entity
     * @param loc    the target location
     * @return true if allowed
     */
    public boolean canTeleport(Entity entity, Location loc) {
        return check(entity, loc, RequiredTrust.ACCESSOR, FLAG_TELEPORT);
    }

    /**
     * Checks if a grave can be looted at the given location.
     *
     * @param entity the acting entity
     * @param loc    the target location
     * @return true if allowed
     */
    public boolean canLoot(Entity entity, Location loc) {
        return check(entity, loc, RequiredTrust.CONTAINER, FLAG_LOOT);
    }

    /**
     * Checks if a grave can be auto-looted at the given location.
     *
     * @param entity the acting entity
     * @param loc    the target location
     * @return true if allowed
     */
    public boolean canAutoLoot(Entity entity, Location loc) {
        return check(entity, loc, RequiredTrust.CONTAINER, FLAG_AUTOLOOT);
    }

    /**
     * Checks if an entity is allowed to walk over a grave at the given location.
     *
     * @param entity the acting entity
     * @param loc    the target location
     * @return true if allowed
     */
    public boolean canWalkOver(Entity entity, Location loc) {
        return check(entity, loc, RequiredTrust.ACCESSOR, FLAG_WALKOVER);
    }

    /**
     * Checks if projectiles can impact a grave at the given location.
     *
     * @param entity the acting entity
     * @param loc    the target location
     * @return true if allowed
     */
    public boolean canProjectile(Entity entity, Location loc) {
        return check(entity, loc, RequiredTrust.ACCESSOR, FLAG_PROJECTILE);
    }

    /**
     * Checks if player can break a grave at the given location.
     *
     * @param entity the acting entity
     * @param loc    the target location
     * @return true if allowed
     */
    public boolean canBreak(Entity entity, Location loc) {
        return check(entity, loc, RequiredTrust.BUILDER, FLAG_BREAK);
    }

    /**
     * Checks if a player is considered a member of a claim represented by a region key.
     *
     * @param regionKey the region key string
     * @param player    the player
     * @return true if the player is a member (>= accessor trust)
     */
    public boolean isMember(String regionKey, Player player) {
        if (player == null || regionKey == null) return false;
        if (!regionKey.toLowerCase(Locale.ROOT).startsWith("griefdefender|")) return false;

        String[] parts = regionKey.split("\\|");
        if (parts.length < 3) return false;

        UUID claimId;
        try {
            claimId = UUID.fromString(parts[2]);
        } catch (IllegalArgumentException ex) {
            return false;
        }

        Claim claim = GriefDefender.getCore().getClaim(claimId);
        if (claim == null || claim.isWilderness()) return false;

        UUID uuid = player.getUniqueId();
        return uuid.equals(claim.getOwnerUniqueId())
                || claim.isUserTrusted(uuid, TrustTypes.MANAGER)
                || claim.isUserTrusted(uuid, TrustTypes.BUILDER)
                || claim.isUserTrusted(uuid, TrustTypes.CONTAINER)
                || claim.isUserTrusted(uuid, TrustTypes.ACCESSOR);
    }

    /**
     * Builds a list of region keys for a claim at a location.
     *
     * @param location the target location
     * @return a list of region keys
     */
    public List<String> getRegionKeyList(Location location) {
        List<String> keys = new ArrayList<>();
        if (location == null) return keys;

        Claim claim = GriefDefender.getCore().getClaimAt(location);
        if (claim == null || claim.isWilderness()) return keys;

        World world = location.getWorld();
        String worldName = world != null ? world.getName() : "world";
        Claim parent = claim.getParent();

        if (parent != null) {
            keys.add("griefdefender|" + worldName + "|" + parent.getUniqueId());
            keys.add("griefdefender|" + worldName + "|" + parent.getUniqueId() + "|sub:" + claim.getUniqueId());
        } else {
            keys.add("griefdefender|" + worldName + "|" + claim.getUniqueId());
        }
        return keys;
    }

    private enum RequiredTrust { ACCESSOR, CONTAINER, BUILDER }

    /**
     * Core check method that evaluates custom flags and falls back to trust roles.
     *
     * @param entity        the acting entity
     * @param loc           the target location
     * @param fallbackTrust the trust level required if flags are UNDEFINED
     * @param flagIds       the flag ids to evaluate
     * @return true if allowed
     */
    private boolean check(Entity entity, Location loc, RequiredTrust fallbackTrust, String... flagIds) {
        if (!(entity instanceof Player)) return true;
        if (loc == null) return true;

        Player player = (Player) entity;
        UUID uuid = player.getUniqueId();

        final Claim claim = GriefDefender.getCore().getClaimAt(loc);
        if (claim == null || claim.isWilderness()) return true;

        if (uuid.equals(claim.getOwnerUniqueId())) return true;

        Tristate result = evaluateFlags(player, claim, loc, flagIds);
        if (result == Tristate.TRUE)  return true;
        if (result == Tristate.FALSE) return false;

        switch (fallbackTrust) {
            case ACCESSOR:
                return claim.isUserTrusted(uuid, TrustTypes.ACCESSOR)
                        || claim.isUserTrusted(uuid, TrustTypes.CONTAINER)
                        || claim.isUserTrusted(uuid, TrustTypes.BUILDER)
                        || claim.isUserTrusted(uuid, TrustTypes.MANAGER);
            case CONTAINER:
                return claim.isUserTrusted(uuid, TrustTypes.CONTAINER)
                        || claim.isUserTrusted(uuid, TrustTypes.BUILDER)
                        || claim.isUserTrusted(uuid, TrustTypes.MANAGER);
            case BUILDER:
                return claim.isUserTrusted(uuid, TrustTypes.BUILDER)
                        || claim.isUserTrusted(uuid, TrustTypes.MANAGER);
            default:
                return false;
        }
    }

    /**
     * Evaluates one or more custom flags for a player at a location within a claim.
     *
     * @param player   the player
     * @param claim    the claim
     * @param loc      the location
     * @param flagIds  the flag identifiers
     * @return the combined flag result (TRUE, FALSE, or UNDEFINED)
     */
    private Tristate evaluateFlags(Player player, Claim claim, Location loc, String... flagIds) {
        if (player == null || claim == null || loc == null || flagIds == null || flagIds.length == 0) {
            return Tristate.UNDEFINED;
        }

        final PermissionManager pm = GriefDefender.getPermissionManager();
        final Subject subject = GriefDefender.getCore().getDefaultSubject();
        final Set<Context> contexts = Collections.emptySet();

        boolean sawDeny = false;

        for (String rawId : flagIds) {
            if (rawId == null) continue;
            final String id = rawId.trim();
            if (id.isEmpty()) continue;

            Flag flag = resolveFlag(id);
            if (flag == null) {
                flag = resolveFlag(id.toLowerCase(Locale.ROOT));
                if (flag == null) continue;
            }

            Tristate r = pm.getActiveFlagPermissionValue(
                    claim,
                    subject,
                    flag,
                    player,
                    loc,
                    contexts
            );

            if (r == Tristate.TRUE)  return Tristate.TRUE;
            if (r == Tristate.FALSE) sawDeny = true;
        }

        return sawDeny ? Tristate.FALSE : Tristate.UNDEFINED;
    }

    /**
     * Resolves a Flag object from the registry by id or "gravesx:" namespace.
     *
     * @param key the flag id
     * @return the resolved Flag, or null if not found
     */
    private Flag resolveFlag(String key) {
        return GriefDefender.getRegistry()
                .getType(Flag.class, key)
                .or(() -> GriefDefender.getRegistry().getType(Flag.class, "gravesx:" + key))
                .orElse(null);
    }
}
