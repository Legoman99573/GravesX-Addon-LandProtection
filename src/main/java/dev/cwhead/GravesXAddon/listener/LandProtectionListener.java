package dev.cwhead.GravesXAddon.listener;

import dev.cwhead.GravesX.event.*;
import dev.cwhead.GravesXAddon.LandProtection;
import dev.cwhead.GravesXAddon.config.LandProtectionMessagesConfig;
import org.bukkit.Location;
import org.bukkit.entity.Player;
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

    private void deny(Player p, Action a, CancellableGravesEvent e) {
        p.sendMessage(messages.denyMessageForAction(a.key, a.defaultText));
        e.setCancelled(true);
    }

    private void debugAllowed(Player p, Location loc, Action a) {
        plugin.getGravesXAPI().plugin().debugMessage(p.getDisplayName() + " can " + a.name().toLowerCase() + " at: " + loc, 2);
    }

    /**
     * Returns true if the action is allowed across all enabled protection systems.
     * For each system: allowed if (permission check passes) OR (membership check passes).
     * If any enabled system denies, we return false.
     */
    private boolean isAllowedEverywhere(Player player, Location loc, Action action) {
        boolean allowed = true;

        if (plugin.isWorldGuardEnabled() && plugin.getWorldGuard() != null) {
            allowed = checkWG(player, loc, action);
            if (!allowed) return false;
        }

        if (plugin.isTownyEnabled() && plugin.getTowny() != null) {
            allowed = checkTowny(player, loc, action);
            if (!allowed) return false;
        }

        if (plugin.isLandsEnabled() && plugin.getLands() != null) {
            allowed = checkLands(player, loc, action);
            if (!allowed) return false;
        }

        if (plugin.isGriefDefenderEnabled() && plugin.getGriefDefender() != null) {
            allowed = checkGD(player, loc, action);
            if (!allowed) return false;
        }

        if (plugin.isGriefPreventionEnabled() && plugin.getGriefPrevention() != null) {
            allowed = checkGP(player, loc, action);
            if (!allowed) return false;
        }

        return allowed;
    }

    private boolean checkWG(Player p, Location loc, Action a) {
        boolean perm;
        switch (a) {
            case CREATE:     perm = plugin.getWorldGuard().canCreateGrave(p, loc); break;
            case TELEPORT:   perm = plugin.getWorldGuard().canTeleport(p, loc);    break;
            case OPEN:       perm = plugin.getWorldGuard().canLoot(p, loc);        break;
            case AUTO_LOOT:  perm = plugin.getWorldGuard().canAutoLoot(p, loc);    break;
            case WALK_OVER:  perm = plugin.getWorldGuard().canWalkOver(p, loc);    break;
            case PROJECTILE: perm = plugin.getWorldGuard().canProjectile(p, loc);  break;
            case BREAK:      perm = plugin.getWorldGuard().canBreak(p, loc);       break;
            default:         perm = true;
        }
        if (perm) return true;

        List<String> keys = plugin.getWorldGuard().getRegionKeyList(loc);
        for (String key : keys) {
            String[] parts = key.split("\\|");
            if (parts.length >= 3 && plugin.getWorldGuard().isMember(parts[2], p)) {
                return true;
            }
        }
        return false;
    }

    private boolean checkTowny(Player p, Location loc, Action a) {
        boolean perm;
        switch (a) {
            case CREATE:     perm = plugin.getTowny().canCreateGrave(p, loc);   break;
            case TELEPORT:   perm = plugin.getTowny().canTeleport(p, loc);      break;
            case OPEN:       perm = plugin.getTowny().canLoot(p, loc);          break;
            case AUTO_LOOT:  perm = plugin.getTowny().canAutoLoot(p, loc);      break;
            case WALK_OVER:  perm = plugin.getTowny().canWalkOver(p, loc);      break;
            case PROJECTILE: perm = plugin.getTowny().canProjectile(p, loc);    break;
            case BREAK:      perm = plugin.getTowny().canBreak(p, loc);         break;
            default:         perm = true;
        }
        if (perm) return true;

        return plugin.getTowny().isResident(p, loc);
    }

    private boolean checkLands(Player p, Location loc, Action a) {
        boolean perm;
        switch (a) {
            case CREATE:     perm = plugin.getLands().canCreateGrave(p, loc);   break;
            case TELEPORT:   perm = plugin.getLands().canTeleport(p, loc);      break;
            case OPEN:       perm = plugin.getLands().canLoot(p, loc);          break;
            case AUTO_LOOT:  perm = plugin.getLands().canAutoLoot(p, loc);      break;
            case WALK_OVER:  perm = plugin.getLands().canWalkOver(p, loc);      break;
            case PROJECTILE: perm = plugin.getLands().canProjectile(p, loc);    break;
            case BREAK:      perm = plugin.getLands().canBreak(p, loc);         break;
            default:         perm = true;
        }
        if (perm) return true;

        List<String> keys = plugin.getLands().getRegionKeyList(loc);
        for (String key : keys) {
            if (plugin.getLands().isMember(key, p)) return true;
        }
        return false;
    }

    private boolean checkGD(Player p, Location loc, Action a) {
        boolean perm;
        switch (a) {
            case CREATE:     perm = plugin.getGriefDefender().canCreateGrave(p, loc);   break;
            case TELEPORT:   perm = plugin.getGriefDefender().canTeleport(p, loc);      break;
            case OPEN:       perm = plugin.getGriefDefender().canLoot(p, loc);          break;
            case AUTO_LOOT:  perm = plugin.getGriefDefender().canAutoLoot(p, loc);      break;
            case WALK_OVER:  perm = plugin.getGriefDefender().canWalkOver(p, loc);      break;
            case PROJECTILE: perm = plugin.getGriefDefender().canProjectile(p, loc);    break;
            case BREAK:      perm = plugin.getGriefDefender().canBreak(p, loc);         break;
            default:         perm = true;
        }
        if (perm) return true;

        List<String> keys = plugin.getGriefDefender().getRegionKeyList(loc);
        for (String key : keys) {
            if (plugin.getGriefDefender().isMember(key, p)) return true;
        }
        return false;
    }

    private boolean checkGP(Player p, Location loc, Action a) {
        boolean perm;
        switch (a) {
            case CREATE:     perm = plugin.getGriefPrevention().canCreateGrave(p, loc);   break;
            case TELEPORT:   perm = plugin.getGriefPrevention().canTeleport(p, loc);      break;
            case OPEN:       perm = plugin.getGriefPrevention().canLoot(p, loc);          break;
            case AUTO_LOOT:  perm = plugin.getGriefPrevention().canAutoLoot(p, loc);      break;
            case WALK_OVER:  perm = plugin.getGriefPrevention().canWalkOver(p, loc);      break;
            case PROJECTILE: perm = plugin.getGriefPrevention().canProjectile(p, loc);    break;
            case BREAK:      perm = plugin.getGriefPrevention().canBreak(p, loc);         break;
            default:         perm = true;
        }
        if (perm) return true;

        List<String> keys = plugin.getGriefPrevention().getRegionKeyList(loc);
        for (String key : keys) {
            if (plugin.getGriefPrevention().isMember(key, p)) return true;
        }
        return false;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onGraveCreate(GraveCreateEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        Location loc = player.getLocation();

        if (!isAllowedEverywhere(player, loc, Action.CREATE)) {
            deny(player, event);
        } else {
            debugAllowed(player, loc, Action.CREATE);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onGraveTeleport(GraveTeleportEvent event) {
        Player player = event.getPlayer();
        if (player == null) return;
        Location loc = player.getLocation();

        if (!isAllowedEverywhere(player, loc, Action.TELEPORT)) {
            deny(player, event);
        } else {
            debugAllowed(player, loc, Action.TELEPORT);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onGraveOpen(GraveOpenEvent event) {
        Player player = event.getPlayer();
        if (player == null) return;
        Location loc = player.getLocation();

        if (!isAllowedEverywhere(player, loc, Action.OPEN)) {
            deny(player, event);
        } else {
            debugAllowed(player, loc, Action.OPEN);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onGraveAutoLooted(GraveAutoLootEvent event) {
        Player player = event.getPlayer();
        if (player == null) return;
        Location loc = player.getLocation();

        if (!isAllowedEverywhere(player, loc, Action.AUTO_LOOT)) {
            deny(player, event);
        } else {
            debugAllowed(player, loc, Action.AUTO_LOOT);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onGraveWalkedOver(GraveWalkOverEvent event) {
        Player player = event.getPlayer();
        if (player == null) return;
        Location loc = player.getLocation();

        if (!isAllowedEverywhere(player, loc, Action.WALK_OVER)) {
            deny(player, event);
        } else {
            debugAllowed(player, loc, Action.WALK_OVER);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onGraveProjectile(GraveProjectileHitEvent event) {
        Player player = event.getPlayer();
        if (player == null) return;
        Location loc = player.getLocation();

        if (!isAllowedEverywhere(player, loc, Action.PROJECTILE)) {
            deny(player, event);
        } else {
            debugAllowed(player, loc, Action.PROJECTILE);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onGraveBreak(GraveBreakEvent event) {
        Player player = event.getPlayer();
        if (player == null) return;
        Location loc = player.getLocation();

        if (!isAllowedEverywhere(player, loc, Action.BREAK)) {
            deny(player, event);
        } else {
            debugAllowed(player, loc, Action.BREAK);
        }
    }

    private interface CancellableGravesEvent {
        void setCancelled(boolean cancelled);
    }

    private static CancellableGravesEvent adapt(GraveCreateEvent e) {
        return e::setCancelled;
    }

    private static CancellableGravesEvent adapt(GraveTeleportEvent e) {
        return e::setCancelled;
    }

    private static CancellableGravesEvent adapt(GraveOpenEvent e) {
        return e::setCancelled;
    }

    private static CancellableGravesEvent adapt(GraveAutoLootEvent e) {
        return e::setCancelled;
    }

    private static CancellableGravesEvent adapt(GraveWalkOverEvent e) {
        return e::setCancelled;
    }

    private static CancellableGravesEvent adapt(GraveProjectileHitEvent e) {
        return e::setCancelled;
    }

    private static CancellableGravesEvent adapt(GraveBreakEvent e) {
        return e::setCancelled;
    }

    private void deny(Player p, GraveCreateEvent e) {
        deny(p, Action.CREATE, adapt(e));
    }

    private void deny(Player p, GraveTeleportEvent e) {
        deny(p, Action.TELEPORT, adapt(e));
    }

    private void deny(Player p, GraveOpenEvent e) {
        deny(p, Action.OPEN, adapt(e));
    }
    private void deny(Player p, GraveAutoLootEvent e) {
        deny(p, Action.AUTO_LOOT, adapt(e));
    }
    private void deny(Player p, GraveWalkOverEvent e) {
        deny(p, Action.WALK_OVER, adapt(e));
    }
    private void deny(Player p, GraveProjectileHitEvent e) {
        deny(p, Action.PROJECTILE, adapt(e));
    }
    private void deny(Player p, GraveBreakEvent e) {
        deny(p, Action.BREAK, adapt(e));
    }
}
