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
import dev.cwhead.GravesXAddon.config.LandProtectionGriefDefenderConfig;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.*;

public class GriefDefenderImpl {

    private static final String FLAG_CREATE     = "gravesx-grave-create";
    private static final String FLAG_TELEPORT   = "gravesx-grave-teleport";
    private static final String FLAG_LOOT       = "gravesx-grave-loot";
    private static final String FLAG_AUTOLOOT   = "gravesx-grave-autoloot";
    private static final String FLAG_WALKOVER   = "gravesx-grave-walkover";
    private static final String FLAG_PROJECTILE = "gravesx-grave-projectile";
    private static final String FLAG_BREAK      = "gravesx-grave-break";

    private static final String NAMESPACE = "gravesx";

    private final LandProtection plugin;
    private final LandProtectionGriefDefenderConfig config;

    public GriefDefenderImpl(LandProtection plugin) {
        this.plugin = plugin;
        this.config = new LandProtectionGriefDefenderConfig();
    }

    public void reloadConfig() {
        config.reload();
    }

    public boolean canCreateGrave(Entity e, Location l) {
        return check(e, l, requiredTrust("create"), FLAG_CREATE);
    }

    public boolean canTeleport (Entity e, Location l) {
        return check(e, l, requiredTrust("teleport"), FLAG_TELEPORT);
    }

    public boolean canLoot (Entity e, Location l) {
        return check(e, l, requiredTrust("loot"), FLAG_LOOT);
    }

    public boolean canAutoLoot (Entity e, Location l) {
        return check(e, l, requiredTrust("autoloot"), FLAG_AUTOLOOT);
    }

    public boolean canWalkOver (Entity e, Location l) {
        return check(e, l, requiredTrust("walkover"), FLAG_WALKOVER);
    }

    public boolean canProjectile (Entity e, Location l) {
        return check(e, l, requiredTrust("projectile"), FLAG_PROJECTILE);
    }

    public boolean canBreak (Entity e, Location l) {
        return check(e, l, requiredTrust("break"), FLAG_BREAK);
    }

    public boolean isMember(String regionKey, Player player) {
        if (player == null || regionKey == null) return false;
        if (!regionKey.toLowerCase(Locale.ROOT).startsWith("griefdefender|")) return false;

        String[] parts = regionKey.split("\\|");
        if (parts.length < 3) return false;

        UUID claimId;
        try { claimId = UUID.fromString(parts[2]); } catch (IllegalArgumentException ex) { return false; }

        Claim claim = GriefDefender.getCore().getClaim(claimId);
        if (claim == null || claim.isWilderness()) return false;

        UUID uuid = player.getUniqueId();
        return uuid.equals(claim.getOwnerUniqueId())
                || claim.isUserTrusted(uuid, TrustTypes.MANAGER)
                || claim.isUserTrusted(uuid, TrustTypes.BUILDER)
                || claim.isUserTrusted(uuid, TrustTypes.CONTAINER)
                || claim.isUserTrusted(uuid, TrustTypes.ACCESSOR);
    }

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

    private RequiredTrust requiredTrust(String actionKey) {
        String s = config.trustLevelFor(actionKey);
        if (s == null) {
            switch (actionKey) {
                case "create":
                case "break":
                    return RequiredTrust.BUILDER;
                case "loot":
                case "autoloot":
                    return RequiredTrust.CONTAINER;
                default:
                    return RequiredTrust.ACCESSOR;
            }
        }
        try {
            return RequiredTrust.valueOf(s.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return RequiredTrust.ACCESSOR;
        }
    }

    private boolean check(Entity entity, Location loc, RequiredTrust fallbackTrust, String... flagIds) {
        if (!(entity instanceof Player))
            return config.allowNonPlayer();
        if (loc == null || loc.getWorld() == null)
            return config.invalidLocationAllowed();

        Player player = (Player) entity;
        UUID uuid = player.getUniqueId();

        final Claim claim = GriefDefender.getCore().getClaimAt(loc);
        if (claim == null || claim.isWilderness())
            return config.wildernessAllowed();

        if (config.ownerBypass() && uuid.equals(claim.getOwnerUniqueId()))
            return true;

        LandProtectionGriefDefenderConfig.EvalOrder order = config.evaluationOrder();

        if (order == LandProtectionGriefDefenderConfig.EvalOrder.FLAGS_THEN_TRUST) {
            Tristate r = evaluateFlags(player, claim, loc, flagIds);
            if (r == Tristate.TRUE) return true;
            if (r == Tristate.FALSE) return false;

            return checkTrust(claim, uuid, fallbackTrust);
        } else {
            if (checkTrust(claim, uuid, fallbackTrust)) return true;
            Tristate r = evaluateFlags(player, claim, loc, flagIds);
            if (r == Tristate.TRUE) return true;
            if (r == Tristate.FALSE) return false;

            return config.missingFlagAllowed();
        }
    }

    private boolean checkTrust(Claim claim, UUID uuid, RequiredTrust level) {
        switch (level) {
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

    private Tristate evaluateFlags(Player player, Claim claim, Location loc, String... flagIds) {
        if (!config.flagsEnabled()) return Tristate.UNDEFINED;
        if (player == null || claim == null || loc == null || flagIds == null || flagIds.length == 0) {
            return Tristate.UNDEFINED;
        }

        final PermissionManager pm = GriefDefender.getPermissionManager();
        final Subject subject = GriefDefender.getCore().getDefaultSubject();
        final Set<Context> contexts = Collections.emptySet();

        boolean sawDeny = false;

        for (String rawId : flagIds) {
            if (rawId == null) continue;
            String id = rawId.trim();
            if (id.isEmpty()) continue;

            Flag flag = resolveFlag(id);
            if (flag == null) continue;

            Tristate r = pm.getActiveFlagPermissionValue(claim, subject, flag, player, loc, contexts);
            if (r == Tristate.TRUE)  return Tristate.TRUE;
            if (r == Tristate.FALSE) sawDeny = true;
        }

        if (sawDeny && config.flagsDenyOverrides()) return Tristate.FALSE;
        return Tristate.UNDEFINED;
    }

    private Flag resolveFlag(String key) {
        return GriefDefender.getRegistry()
                .getType(Flag.class, key)
                .or(() -> GriefDefender.getRegistry().getType(Flag.class, NAMESPACE + ":" + key))
                .orElse(null);
    }

    public LandProtectionGriefDefenderConfig getConfig() {
        return config;
    }
}
