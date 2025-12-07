package dev.cwhead.GravesXAddon.listener;

import dev.cwhead.GravesX.event.*;
import dev.cwhead.GravesXAddon.LandProtection;
import dev.cwhead.GravesXAddon.config.LandProtectionMessagesConfig;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.List;

public class LandProtectionListener implements Listener {

    private final LandProtection plugin;
    private final LandProtectionMessagesConfig messages;

    public LandProtectionListener(LandProtection plugin) {
        this.plugin = plugin;
        this.messages = new LandProtectionMessagesConfig(plugin);
    }

    private enum Action {
        CREATE("create", "create a grave here."),
        TELEPORT("teleport", "teleport to your grave in this region."),
        OPEN("open", "open a grave in this region."),
        AUTO_LOOT("auto_loot", "auto loot a grave in this region."),
        WALK_OVER("walk_over", "walk over a grave in this region."),
        PROJECTILE("projectile", "use a projectile on a grave in this region."),
        BREAK("break", "break a grave in this region.");

        final String key;
        final String defaultText;

        Action(String key, String defaultText) {
            this.key = key;
            this.defaultText = defaultText;
        }
    }

    private void deny(Player p, Action a, Cancellable e) {
        p.sendMessage(messages.denyMessageForAction(a.key, a.defaultText));
        e.setCancelled(true);
    }

    private void debugAllowed(Player p, Location loc, Action a) {
        plugin.getGravesXAPI().plugin().debugMessage(
                p.getDisplayName() + " can " + a.name().toLowerCase() + " at: " + loc,
                2
        );
    }

    /**
     * Returns true if the action is allowed across all enabled protection systems.
     *
     * WorldGuard:
     *  - If the player is a member of any applicable region at the location -> always allowed.
     *  - Otherwise, non-members rely on the flag allow/deny via canX().
     *
     * Other systems:
     *  - Behavior is delegated to their canX()/isMember helpers.
     *
     * If any enabled system denies, this returns false.
     */
    private boolean isAllowedEverywhere(Player player, Location loc, Action action) {
        boolean allowed;

        if (plugin.isWorldGuardEnabled() && plugin.getWorldGuard() != null) {
            allowed = checkWG(player, loc, action);
            if (allowed) return true;
        }

        if (plugin.isTownyEnabled() && plugin.getTowny() != null) {
            allowed = checkTowny(player, loc, action);
            if (allowed) return true;
        }

        if (plugin.isLandsEnabled() && plugin.getLands() != null) {
            allowed = checkLands(player, loc, action);
            if (allowed) return true;
        }

        if (plugin.isGriefDefenderEnabled() && plugin.getGriefDefender() != null) {
            allowed = checkGD(player, loc, action);
            if (allowed) return true;
        }

        if (plugin.isGriefPreventionEnabled() && plugin.getGriefPrevention() != null) {
            allowed = checkGP(player, loc, action);
            if (allowed) return true;
        }

        return false;
    }

    /**
     * WorldGuard semantics:
     *  - If player is a member of ANY applicable region -> always allowed.
     *  - Otherwise -> rely on WG flags via canX().
     */
    private boolean checkWG(Player p, Location loc, Action a) {
        List<String> keys = plugin.getWorldGuard().getRegionKeyList(loc);
        for (String key : keys) {
            String[] parts = key.split("\\|");
            if (parts.length >= 3) {
                String regionId = parts[2];
                if (plugin.getWorldGuard().isMember(regionId, p)) {
                    return true;
                }
            }
        }

        return switch (a) {
            case CREATE -> plugin.getWorldGuard().canCreateGrave(p, loc);
            case TELEPORT -> plugin.getWorldGuard().canTeleport(p, loc);
            case OPEN -> plugin.getWorldGuard().canLoot(p, loc);
            case AUTO_LOOT -> plugin.getWorldGuard().canAutoLoot(p, loc);
            case WALK_OVER -> plugin.getWorldGuard().canWalkOver(p, loc);
            case PROJECTILE -> plugin.getWorldGuard().canProjectile(p, loc);
            case BREAK -> plugin.getWorldGuard().canBreak(p, loc);
        };
    }

    private boolean checkTowny(Player p, Location loc, Action a) {
        boolean perm = switch (a) {
            case CREATE -> plugin.getTowny().canCreateGrave(p, loc);
            case TELEPORT -> plugin.getTowny().canTeleport(p, loc);
            case OPEN -> plugin.getTowny().canLoot(p, loc);
            case AUTO_LOOT -> plugin.getTowny().canAutoLoot(p, loc);
            case WALK_OVER -> plugin.getTowny().canWalkOver(p, loc);
            case PROJECTILE -> plugin.getTowny().canProjectile(p, loc);
            case BREAK -> plugin.getTowny().canBreak(p, loc);
        };
        if (perm) return true;

        return plugin.getTowny().isResident(p, loc);
    }

    private boolean checkLands(Player p, Location loc, Action a) {
        boolean perm = switch (a) {
            case CREATE -> plugin.getLands().canCreateGrave(p, loc);
            case TELEPORT -> plugin.getLands().canTeleport(p, loc);
            case OPEN -> plugin.getLands().canLoot(p, loc);
            case AUTO_LOOT -> plugin.getLands().canAutoLoot(p, loc);
            case WALK_OVER -> plugin.getLands().canWalkOver(p, loc);
            case PROJECTILE -> plugin.getLands().canProjectile(p, loc);
            case BREAK -> plugin.getLands().canBreak(p, loc);
            default -> true;
        };
        if (perm) return true;

        List<String> keys = plugin.getLands().getRegionKeyList(loc);
        for (String key : keys) {
            if (plugin.getLands().isMember(key, p)) return true;
        }
        return false;
    }

    private boolean checkGD(Player p, Location loc, Action a) {
        boolean perm = switch (a) {
            case CREATE -> plugin.getGriefDefender().canCreateGrave(p, loc);
            case TELEPORT -> plugin.getGriefDefender().canTeleport(p, loc);
            case OPEN -> plugin.getGriefDefender().canLoot(p, loc);
            case AUTO_LOOT -> plugin.getGriefDefender().canAutoLoot(p, loc);
            case WALK_OVER -> plugin.getGriefDefender().canWalkOver(p, loc);
            case PROJECTILE -> plugin.getGriefDefender().canProjectile(p, loc);
            case BREAK -> plugin.getGriefDefender().canBreak(p, loc);
        };

        if (perm) return true;

        List<String> keys = plugin.getGriefDefender().getRegionKeyList(loc);
        for (String key : keys) {
            if (plugin.getGriefDefender().isMember(key, p)) return true;
        }
        return false;
    }

    private boolean checkGP(Player p, Location loc, Action a) {
        boolean perm = switch (a) {
            case CREATE -> plugin.getGriefPrevention().canCreateGrave(p, loc);
            case TELEPORT -> plugin.getGriefPrevention().canTeleport(p, loc);
            case OPEN -> plugin.getGriefPrevention().canLoot(p, loc);
            case AUTO_LOOT -> plugin.getGriefPrevention().canAutoLoot(p, loc);
            case WALK_OVER -> plugin.getGriefPrevention().canWalkOver(p, loc);
            case PROJECTILE -> plugin.getGriefPrevention().canProjectile(p, loc);
            case BREAK -> plugin.getGriefPrevention().canBreak(p, loc);
        };

        if (perm) return true;

        List<String> keys = plugin.getGriefPrevention().getRegionKeyList(loc);
        for (String key : keys) {
            if (plugin.getGriefPrevention().isMember(key, p)) return true;
        }
        return false;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onGraveCreate(GraveCreateEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        Location loc = player.getLocation();

        if (isAllowedEverywhere(player, loc, Action.CREATE)) {
            deny(player, Action.CREATE, event);
        } else {
            debugAllowed(player, loc, Action.CREATE);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onGraveTeleport(GraveTeleportEvent event) {
        Player player = event.getPlayer();
        if (player == null) return;
        Location loc = player.getLocation();

        if (isAllowedEverywhere(player, loc, Action.TELEPORT)) {
            deny(player, Action.TELEPORT, event);
        } else {
            debugAllowed(player, loc, Action.TELEPORT);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onGraveOpen(GraveOpenEvent event) {
        Player player = event.getPlayer();
        if (player == null) return;
        Location loc = player.getLocation();

        if (isAllowedEverywhere(player, loc, Action.OPEN)) {
            deny(player, Action.OPEN, event);
        } else {
            debugAllowed(player, loc, Action.OPEN);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onGraveAutoLooted(GraveAutoLootEvent event) {
        Player player = event.getPlayer();
        if (player == null) return;
        Location loc = player.getLocation();

        if (isAllowedEverywhere(player, loc, Action.AUTO_LOOT)) {
            deny(player, Action.AUTO_LOOT, event);
        } else {
            debugAllowed(player, loc, Action.AUTO_LOOT);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onGraveWalkedOver(GraveWalkOverEvent event) {
        Player player = event.getPlayer();
        if (player == null) return;
        Location loc = player.getLocation();

        if (isAllowedEverywhere(player, loc, Action.WALK_OVER)) {
            deny(player, Action.WALK_OVER, event);
        } else {
            debugAllowed(player, loc, Action.WALK_OVER);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onGraveProjectile(GraveProjectileHitEvent event) {
        Player player = event.getPlayer();
        if (player == null) return;
        Location loc = player.getLocation();

        if (isAllowedEverywhere(player, loc, Action.PROJECTILE)) {
            deny(player, Action.PROJECTILE, event);
        } else {
            debugAllowed(player, loc, Action.PROJECTILE);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onGraveBreak(GraveBreakEvent event) {
        Player player = event.getPlayer();
        if (player == null) return;
        Location loc = player.getLocation();

        if (isAllowedEverywhere(player, loc, Action.BREAK)) {
            deny(player, Action.BREAK, event);
        } else {
            debugAllowed(player, loc, Action.BREAK);
        }
    }
}