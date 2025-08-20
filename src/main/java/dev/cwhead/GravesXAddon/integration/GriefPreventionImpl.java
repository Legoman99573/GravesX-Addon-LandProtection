package dev.cwhead.GravesXAddon.integration;

import dev.cwhead.GravesXAddon.LandProtection;
import dev.cwhead.GravesXAddon.config.LandProtectionGriefPreventionConfig;
import dev.cwhead.GravesXAddon.listener.dependencies.GriefPreventionListener;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.*;

public class GriefPreventionImpl {

    private static final String FLAG_CREATE = "gravesx-grave-create";
    private static final String FLAG_TELEPORT = "gravesx-grave-teleport";
    private static final String FLAG_LOOT = "gravesx-grave-loot";
    private static final String FLAG_AUTOLOOT = "gravesx-grave-autoloot";
    private static final String FLAG_WALKOVER = "gravesx-grave-walkover";
    private static final String FLAG_PROJECTILE = "gravesx-grave-projectile";
    private static final String FLAG_BREAK = "gravesx-grave-break";

    private final LandProtection plugin;
    private final LandProtectionGriefPreventionConfig config;

    private final boolean hasGriefPrevention;
    private final boolean flagsPresent;
    private final boolean gpFlagsPresent;

    private final boolean useFlags;
    private final boolean useGPFlags;

    private Object registrar;
    private Object gpFlagsManager;

    public GriefPreventionImpl(LandProtection plugin) {
        this.plugin = plugin;
        this.config = new LandProtectionGriefPreventionConfig();

        plugin.getServer().getPluginManager().registerEvents(new GriefPreventionListener(plugin), plugin);

        this.hasGriefPrevention = Bukkit.getPluginManager().getPlugin("GriefPrevention") != null;
        this.flagsPresent = Bukkit.getPluginManager().getPlugin("Flags") != null;
        this.gpFlagsPresent = Bukkit.getPluginManager().getPlugin("GPFlags") != null
                || Bukkit.getPluginManager().getPlugin("GriefPreventionFlags") != null;

        this.useFlags = flagsPresent   && config.useFlags();
        this.useGPFlags = gpFlagsPresent && config.useGPFlags();

        if (useFlags && config.registerFlags()) {
            initFlagsAPI();
        }
        if (useGPFlags && config.registerGPFlags()) {
            initGPFlagsAPI();
        }
    }

    public void reloadConfig() {
        config.reload();
    }

    public boolean canCreateGrave(Entity entity, Location loc) {
        return check(entity, loc, requiredTrust("create"), FLAG_CREATE);
    }
    public boolean canTeleport(Entity entity, Location loc) {
        return check(entity, loc, requiredTrust("teleport"), FLAG_TELEPORT);
    }
    public boolean canLoot(Entity entity, Location loc) {
        return check(entity, loc, requiredTrust("loot"), FLAG_LOOT);
    }
    public boolean canAutoLoot(Entity entity, Location loc) {
        return check(entity, loc, requiredTrust("autoloot"), FLAG_AUTOLOOT);
    }
    public boolean canWalkOver(Entity entity, Location loc) {
        return check(entity, loc, requiredTrust("walkover"), FLAG_WALKOVER);
    }
    public boolean canProjectile(Entity entity, Location loc) {
        return check(entity, loc, requiredTrust("projectile"), FLAG_PROJECTILE);
    }
    public boolean canBreak(Entity entity, Location loc) {
        return check(entity, loc, requiredTrust("break"), FLAG_BREAK);
    }

    public List<String> getRegionKeyList(Location location) {
        List<String> keys = new ArrayList<>();
        if (!hasGriefPrevention || location == null) return keys;

        Object claim = getGPClaimAt(location);
        if (claim == null) return keys;

        UUID id = getGPClaimId(claim);
        if (id == null) return keys;

        World world = location.getWorld();
        String worldName = world != null ? world.getName() : "world";

        Object parent = getGPParent(claim);
        if (parent != null) {
            UUID pid = getGPClaimId(parent);
            if (pid != null) {
                keys.add("griefprevention|" + worldName + "|" + pid);
                keys.add("griefprevention|" + worldName + "|" + pid + "|sub:" + id);
            }
        } else {
            keys.add("griefprevention|" + worldName + "|" + id);
        }
        return keys;
    }

    public boolean isMember(String regionKey, Player player) {
        if (!hasGriefPrevention || player == null || regionKey == null) return false;
        if (!regionKey.toLowerCase(Locale.ROOT).startsWith("griefprevention|")) return false;

        String[] parts = regionKey.split("\\|");
        if (parts.length < 3) return false;

        UUID claimId;
        try { claimId = UUID.fromString(parts[2]); } catch (IllegalArgumentException ex) { return false; }

        Object claim = getGPClaimById(claimId);
        if (claim == null || isWilderness(claim)) return false;

        UUID uuid = player.getUniqueId();
        return isOwner(claim, uuid)
                || isTrusted(claim, uuid, "AccessTrust")
                || isTrusted(claim, uuid, "ContainerTrust")
                || isTrusted(claim, uuid, "BuildTrust")
                || isTrusted(claim, uuid, "Manage");
    }

    private enum RequiredTrust { ACCESSOR, CONTAINER, BUILDER }
    private enum Tri { TRUE, FALSE, UNDEFINED }

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
        try { return RequiredTrust.valueOf(s.toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException e) { return RequiredTrust.ACCESSOR; }
    }

    private boolean check(Entity entity, Location loc, RequiredTrust fallbackTrust, String... flagIds) {
        if (!(entity instanceof Player)) return config.allowNonPlayer();
        if (!hasGriefPrevention || loc == null) return config.invalidLocationAllowed();

        Player player = (Player) entity;

        Object claim = getGPClaimAt(loc);
        if (claim == null || isWilderness(claim)) return config.wildernessAllowed();

        if (config.ownerBypass() && isOwner(claim, player.getUniqueId())) return true;

        LandProtectionGriefPreventionConfig.EvalOrder order = config.evaluationOrder();

        if (order == LandProtectionGriefPreventionConfig.EvalOrder.FLAGS_THEN_TRUST) {
            Tri eval = evaluateFlags(player, claim, loc, flagIds);
            if (eval == Tri.TRUE)  return true;
            if (eval == Tri.FALSE) return false;
            return checkTrust(claim, player.getUniqueId(), fallbackTrust);
        } else {
            if (checkTrust(claim, player.getUniqueId(), fallbackTrust)) return true;
            Tri eval = evaluateFlags(player, claim, loc, flagIds);
            if (eval == Tri.TRUE)  return true;
            if (eval == Tri.FALSE) return false;
            return config.missingFlagAllowed();
        }
    }

    private boolean checkTrust(Object claim, UUID uuid, RequiredTrust level) {
        switch (level) {
            case ACCESSOR:
                return isTrusted(claim, uuid, "AccessTrust")
                        || isTrusted(claim, uuid, "ContainerTrust")
                        || isTrusted(claim, uuid, "BuildTrust")
                        || isTrusted(claim, uuid, "Manage");
            case CONTAINER:
                return isTrusted(claim, uuid, "ContainerTrust")
                        || isTrusted(claim, uuid, "BuildTrust")
                        || isTrusted(claim, uuid, "Manage");
            case BUILDER:
                return isTrusted(claim, uuid, "BuildTrust")
                        || isTrusted(claim, uuid, "Manage");
            default:
                return false;
        }
    }

    private Tri evaluateFlags(Player player, Object gpClaim, Location loc, String... flagIds) {
        if (flagIds == null || flagIds.length == 0) return Tri.UNDEFINED;

        boolean sawDeny = false;

        if (useFlags) {
            try {
                Class<?> flagsAPI = Class.forName("io.github.alshain01.flags.api.FlagsAPI");
                Class<?> flagCls = Class.forName("io.github.alshain01.flags.api.Flag");
                Class<?> areaCls = Class.forName("io.github.alshain01.flags.api.area.Area");

                Object area = flagsAPI.getMethod("getAreaAt", Location.class).invoke(null, loc);
                if (area != null) {
                    for (String id : flagIds) {
                        Object f = resolveFlagsFlag(id);
                        if (f == null) continue;
                        Boolean abs = (Boolean) areaCls.getMethod("getAbsoluteState", flagCls).invoke(area, f);
                        if (Boolean.TRUE.equals(abs))  return Tri.TRUE;
                        if (Boolean.FALSE.equals(abs)) sawDeny = true;
                    }
                }
            } catch (Exception ignored) {
                //ignored
            }
        }

        if (useGPFlags) {
            try {
                Class<?> fmCls = Class.forName("net.kaikk.mc.gpf.FlagManager");
                Class<?> flagCls = Class.forName("net.kaikk.mc.gpf.Flag");
                for (String id : flagIds) {
                    Object flag = fmCls.getMethod("getFlag", String.class).invoke(gpFlagsManager, id);
                    if (flag == null) continue;

                    Boolean state = null;
                    try {
                        state = (Boolean) fmCls.getMethod("getFlagState", Location.class, flagCls)
                                .invoke(gpFlagsManager, loc, flag);
                    } catch (NoSuchMethodException ignore) {
                        try {
                            state = (Boolean) fmCls.getMethod("getState", Location.class, flagCls)
                                    .invoke(gpFlagsManager, loc, flag);
                        } catch (NoSuchMethodException ignored2) {
                            //ignored
                        }
                    }

                    if (Boolean.TRUE.equals(state))  return Tri.TRUE;
                    if (Boolean.FALSE.equals(state)) sawDeny = true;
                }
            } catch (Exception ignored) {
                //ignored
            }
        }

        if (sawDeny && config.flagsDenyOverrides()) return Tri.FALSE;
        return Tri.UNDEFINED;
    }

    private void initFlagsAPI() {
        try {
            Class<?> flagsAPI     = Class.forName("io.github.alshain01.flags.api.FlagsAPI");
            Class<?> registrarCls = Class.forName("io.github.alshain01.flags.api.Registrar");
            this.registrar = flagsAPI.getMethod("getRegistrar").invoke(null);

            registerFlagWithFlags(registrarCls, FLAG_CREATE, "Allow creating a grave.", config.flagDefault(FLAG_CREATE, false));
            registerFlagWithFlags(registrarCls, FLAG_TELEPORT, "Allow teleporting to a grave.", config.flagDefault(FLAG_TELEPORT, true));
            registerFlagWithFlags(registrarCls, FLAG_LOOT, "Allow looting a grave.", config.flagDefault(FLAG_LOOT, true));
            registerFlagWithFlags(registrarCls, FLAG_AUTOLOOT, "Allow auto-looting a grave.", config.flagDefault(FLAG_AUTOLOOT, true));
            registerFlagWithFlags(registrarCls, FLAG_WALKOVER, "Allow walking over a grave.", config.flagDefault(FLAG_WALKOVER, true));
            registerFlagWithFlags(registrarCls, FLAG_PROJECTILE, "Allow projectiles to hit a grave.", config.flagDefault(FLAG_PROJECTILE, false));
            registerFlagWithFlags(registrarCls, FLAG_BREAK, "Allow breaking a grave.", config.flagDefault(FLAG_BREAK, false));
        } catch (Throwable ignored) {
            this.registrar = null;
        }
    }

    private void registerFlagWithFlags(Class<?> registrarCls, String id, String desc, boolean def) {
        if (registrar == null) return;
        try {
            registrarCls.getMethod("register", String.class, String.class, boolean.class, String.class)
                    .invoke(registrar, id, desc, def, "GravesX");
        } catch (NoSuchMethodException e) {
            try {
                registrarCls.getMethod("register", String.class, String.class, boolean.class, String.class, String.class, String.class)
                        .invoke(registrar, id, desc, def, "GravesX",
                                "You may " + id.replace("gravesx-grave-", "").replace('-', ' ') + " here.",
                                "You may " + id.replace("gravesx-grave-", "").replace('-', ' ') + " in the wilderness.");
            } catch (Throwable ignored) {
                //ignore
            }
        } catch (Throwable ignored) {
            //ignored
        }
    }

    private Object resolveFlagsFlag(String id) throws Exception {
        if (registrar == null) return null;
        Class<?> registrarCls = Class.forName("io.github.alshain01.flags.api.Registrar");
        try {
            return registrarCls.getMethod("getFlag", String.class).invoke(registrar, id);
        } catch (NoSuchMethodException e) {
            if (config.registerFlags()) {
                registerFlagWithFlags(registrarCls, id, "GravesX flag " + id, config.flagDefault(id, false));
                return registrarCls.getMethod("getFlag", String.class).invoke(registrar, id);
            }
            return null;
        }
    }

    private void initGPFlagsAPI() {
        try {
            Class<?> fmCls = Class.forName("net.kaikk.mc.gpf.FlagManager");
            this.gpFlagsManager = fmCls.getMethod("getInstance").invoke(null);

            if (config.registerGPFlags()) {
                registerFlagWithGPFlags(fmCls, FLAG_CREATE);
                registerFlagWithGPFlags(fmCls, FLAG_TELEPORT);
                registerFlagWithGPFlags(fmCls, FLAG_LOOT);
                registerFlagWithGPFlags(fmCls, FLAG_AUTOLOOT);
                registerFlagWithGPFlags(fmCls, FLAG_WALKOVER);
                registerFlagWithGPFlags(fmCls, FLAG_PROJECTILE);
                registerFlagWithGPFlags(fmCls, FLAG_BREAK);
            }
        } catch (Throwable ignored) {
            this.gpFlagsManager = null;
        }
    }

    private void registerFlagWithGPFlags(Class<?> fmCls, String id) {
        if (gpFlagsManager == null) return;
        try {
            Class<?> flagCls = Class.forName("net.kaikk.mc.gpf.Flag");
            Object existing = null;
            try {
                existing = fmCls.getMethod("getFlag", String.class).invoke(gpFlagsManager, id);
            } catch (Throwable ignore) {}
            if (existing != null) return;

            Object flag = flagCls.getConstructor(String.class).newInstance(id);
            fmCls.getMethod("registerFlag", flagCls).invoke(gpFlagsManager, flag);
        } catch (Throwable ignored) {
            //ignored
        }
    }

    private Object getGPDataStore() {
        try {
            Class<?> gp = Class.forName("me.ryanhamshire.GriefPrevention.GriefPrevention");
            Object inst = gp.getField("instance").get(null);
            return gp.getField("dataStore").get(inst);
        } catch (Throwable t) { return null; }
    }

    private Object getGPClaimAt(Location loc) {
        try {
            Object ds = getGPDataStore();
            if (ds == null) return null;
            return ds.getClass().getMethod("getClaimAt", Location.class, boolean.class, Object.class)
                    .invoke(ds, loc, false, null);
        } catch (Throwable t) { return null; }
    }

    private Object getGPClaimById(UUID id) {
        try {
            Object ds = getGPDataStore();
            if (ds == null) return null;
            return ds.getClass().getMethod("getClaim", UUID.class).invoke(ds, id);
        } catch (Throwable t) { return null; }
    }

    private UUID getGPClaimId(Object claim) {
        try {
            return (UUID) claim.getClass().getField("id").get(claim);
        } catch (Throwable t) {
            try {
                return (UUID) claim.getClass().getMethod("getID").invoke(claim);
            } catch (Throwable ignored) { return null; }
        }
    }

    private Object getGPParent(Object claim) {
        try {
            return claim.getClass().getMethod("getParent").invoke(claim);
        } catch (Throwable t) { return null; }
    }

    private boolean isWilderness(Object claim) {
        try {
            return (boolean) claim.getClass().getMethod("isInWilderness").invoke(claim);
        } catch (Throwable t) { return false; }
    }

    private boolean isOwner(Object claim, UUID uuid) {
        try {
            UUID owner = (UUID) claim.getClass().getMethod("getOwnerID").invoke(claim);
            return uuid.equals(owner);
        } catch (Throwable t) {
            try {
                UUID owner = (UUID) claim.getClass().getField("ownerID").get(claim);
                return uuid.equals(owner);
            } catch (Throwable ignored) {
                return false;
            }
        }
    }

    @SuppressWarnings("unchecked")
    private boolean isTrusted(Object claim, UUID uuid, String trustLevelName) {
        try {
            Class<?> trustLevelCls = Class.forName("me.ryanhamshire.GriefPrevention.TrustLevel");
            Object trustLevel = Enum.valueOf((Class<Enum>) trustLevelCls, trustLevelName);
            return (boolean) claim.getClass().getMethod("hasExplicitPermission", UUID.class, trustLevelCls)
                    .invoke(claim, uuid, trustLevel);
        } catch (Throwable t) {
            try {
                if ("BuildTrust".equals(trustLevelName)) {
                    Set<UUID> builders = (Set<UUID>) claim.getClass().getMethod("getBuilders").invoke(claim);
                    if (builders != null && builders.contains(uuid)) return true;
                }
            } catch (Throwable ignored) {}
            try {
                if ("ContainerTrust".equals(trustLevelName)) {
                    Set<UUID> containers = (Set<UUID>) claim.getClass().getMethod("getContainers").invoke(claim);
                    if (containers != null && containers.contains(uuid)) return true;
                }
            } catch (Throwable ignored) {}
            try {
                if ("AccessTrust".equals(trustLevelName)) {
                    Set<UUID> accessors = (Set<UUID>) claim.getClass().getMethod("getAccessors").invoke(claim);
                    if (accessors != null && accessors.contains(uuid)) return true;
                }
            } catch (Throwable ignored) {}
            return false;
        }
    }

    public LandProtectionGriefPreventionConfig getConfig() {
        return config;
    }
}
