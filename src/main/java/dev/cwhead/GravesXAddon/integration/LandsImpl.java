package dev.cwhead.GravesXAddon.integration;

import dev.cwhead.GravesXAddon.LandProtection;
import dev.cwhead.GravesXAddon.config.LandProtectionLandsConfig;
import me.angeschossen.lands.api.LandsIntegration;
import me.angeschossen.lands.api.flags.enums.FlagTarget;
import me.angeschossen.lands.api.flags.enums.RoleFlagCategory;
import me.angeschossen.lands.api.flags.type.RoleFlag;
import me.angeschossen.lands.api.land.Area;
import me.angeschossen.lands.api.land.Land;
import me.angeschossen.lands.api.land.LandWorld;
import me.angeschossen.lands.api.player.LandPlayer;
import me.angeschossen.lands.api.role.Role;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.*;

public class LandsImpl {

    private final LandsIntegration api;
    private final LandProtectionLandsConfig config;

    private final Map<Action, RoleFlag> flags = new EnumMap<>(Action.class);

    public static final String F_AUTOLOOT = "gravesx-grave-autoloot";
    public static final String F_LOOT = "gravesx-grave-loot";
    public static final String F_CREATE = "gravesx-grave-create";
    public static final String F_TELEPORT = "gravesx-grave-teleport";
    public static final String F_WALKOVER = "gravesx-grave-walkover";
    public static final String F_PROJECTILE = "gravesx-grave-projectile";
    public static final String F_BREAK = "gravesx-grave-break";

    private enum Action {
        CREATE(F_CREATE, "create"),
        TELEPORT(F_TELEPORT, "teleport"),
        LOOT(F_LOOT, "loot"),
        AUTOLOOT(F_AUTOLOOT, "autoloot"),
        WALKOVER(F_WALKOVER, "walkover"),
        PROJECTILE(F_PROJECTILE, "projectile"),
        BREAK(F_BREAK, "break");

        final String flagId;
        final String roleKey;
        Action(String flagId, String roleKey) { this.flagId = flagId; this.roleKey = roleKey; }
    }

    public LandsImpl(LandProtection plugin) {
        this.api = LandsIntegration.of(plugin);
        this.config = new LandProtectionLandsConfig();
        registerFlagsOnLoad(plugin);
    }

    public void reloadConfig() {
        config.reload();
        flags.clear();
    }

    private void registerFlagsOnLoad(LandProtection plugin) {
        api.onLoad(() -> {
            for (Action a : Action.values()) {
                if (!config.isFlagEnabled(a.flagId)) {
                    plugin.getLogger().info("[Lands] Flag disabled by config: " + a.flagId);
                    continue;
                }
                RoleFlag rf = RoleFlag.of(api, FlagTarget.PLAYER, RoleFlagCategory.ACTION, a.flagId)
                        .setDisplayName(pretty(a.flagId))
                        .setDescription(List.of("GravesX Addon control for " + a.roleKey));
                flags.put(a, rf);
                plugin.getLogger().info("[Lands] Registered flag: " + a.flagId);
            }
        });
    }

    private static String pretty(String key) {
        String s = key.replace('-', ' ');
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    public boolean canCreateGrave(Entity e, Location l) {
        return check(e, l, Action.CREATE);
    }
    public boolean canTeleport (Entity e, Location l) {
        return check(e, l, Action.TELEPORT);
    }

    public boolean canLoot (Entity e, Location l) {
        return check(e, l, Action.LOOT);
    }

    public boolean canAutoLoot (Entity e, Location l) {
        return check(e, l, Action.AUTOLOOT);
    }

    public boolean canWalkOver (Entity e, Location l) {
        return check(e, l, Action.WALKOVER);
    }

    public boolean canProjectile (Entity e, Location l) {
        return check(e, l, Action.PROJECTILE);
    }

    public boolean canBreak (Entity e, Location l) {
        return check(e, l, Action.BREAK);
    }

    public boolean isMember(String regionKey, Player player) {
        if (api == null || player == null || regionKey == null || !regionKey.toLowerCase(Locale.ROOT).startsWith("lands|"))
            return false;

        String[] parts = regionKey.split("\\|");
        if (parts.length < 3) return false;

        Land land = api.getLandByName(parts[2]);
        return land != null && land.isTrusted(player.getUniqueId());
    }

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
        if (land != null) keys.add("lands|" + bw.getName() + "|" + land.getName());
        return keys;
    }

    private boolean check(Entity entity, Location loc, Action action) {
        if (!(entity instanceof Player)) return config.allowNonPlayer();
        if (loc == null || loc.getWorld() == null) return config.invalidLocationAllowed();

        LandWorld lw = api.getWorld(loc.getWorld());
        if (lw == null) return config.wildernessAllowed();
        Land land = landAt(lw, loc);
        if (land == null) return config.wildernessAllowed();

        Player p = (Player) entity;

        if (config.rolesEnabled()) {
            Set<String> allowed = config.allowedRolesForAction(action.roleKey);
            if (!allowed.isEmpty()) {
                Set<String> playerRoles = detectRoleTokens(land, p.getUniqueId());
                if (Collections.disjoint(allowed, playerRoles)) {
                    return false;
                }
            }
        }

        RoleFlag flag = flags.get(action);
        if (flag == null) return config.missingFlagAllowed();

        LandPlayer lp = api.getLandPlayer(p.getUniqueId());
        if (lp == null) return false;

        return lw.hasRoleFlag(lp, loc, flag, null, false);
    }

    private Land landAt(LandWorld lw, Location loc) {
        Area area = lw.getArea(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        if (area != null) return area.getLand();
        return api.getLandByChunk(loc.getWorld(), loc.getBlockX() >> 4, loc.getBlockZ() >> 4);
    }

    private boolean isTrusted(Land land, UUID uuid) {
        try {
            Method m = land.getClass().getMethod("isTrusted", UUID.class);
            Object r = m.invoke(land, uuid);
            return r instanceof Boolean && (Boolean) r;
        } catch (Throwable ignored) {}
        return isOwner(land, uuid);
    }

    private boolean isOwner(Land land, UUID uuid) {
        for (String methodName : new String[]{"getOwner", "getOwnerUID", "getOwnerUuid", "getOwnerUniqueId"}) {
            try {
                Method m = land.getClass().getMethod(methodName);
                Object r = m.invoke(land);
                if (r instanceof UUID && uuid.equals(r)) return true;
            } catch (Throwable ignored) {}
        }
        for (String methodName : new String[]{"getOwners", "getOwnerUniqueIds", "getOwnerUuids"}) {
            try {
                Method m = land.getClass().getMethod(methodName);
                Object r = m.invoke(land);
                if (r instanceof Collection) {
                    @SuppressWarnings("unchecked")
                    Collection<UUID> owners = (Collection<UUID>) r;
                    if (owners.contains(uuid)) return true;
                }
            } catch (Throwable ignored) {}
        }
        return false;
    }

    private Set<String> detectRoleTokens(Land land, UUID uuid) {
        Set<String> out = new HashSet<>();

        if (isOwner(land, uuid)) {
            out.add("OWNER");
        }

        if (isTrusted(land, uuid)) {
            out.add("TRUSTED");
        } else {
            out.add("VISITOR");
        }

        String roleName = resolveRoleName(land, uuid);
        if (roleName != null && !roleName.isEmpty()) {
            out.add(roleName.toUpperCase(Locale.ROOT));
        }

        return out;
    }

    private String resolveRoleName(Land land, UUID uuid) {
        try {
            Method m = land.getClass().getMethod("getRole", UUID.class);
            Object r = m.invoke(land, uuid);
            if (r instanceof Role) {
                String name = ((Role) r).getName();
                if (name != null) return name;
            }
        } catch (Throwable ignored) {}

        try {
            LandPlayer lp = api.getLandPlayer(uuid);
            if (lp != null) {
                Method m = lp.getClass().getMethod("getRole", Land.class);
                Object r = m.invoke(lp, land);
                if (r instanceof Role) {
                    String name = ((Role) r).getName();
                    if (name != null) return name;
                }
            }
        } catch (Throwable ignored) {}

        return null;
    }

    public LandProtectionLandsConfig getConfig() {
        return config;
    }
}
