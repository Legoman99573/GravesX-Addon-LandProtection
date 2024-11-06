package dev.cwhead.GravesXAddon.listener;

import com.ranull.graves.event.GraveAutoLootEvent;
import com.ranull.graves.event.GraveCreateEvent;
import com.ranull.graves.event.GraveOpenEvent;
import com.ranull.graves.event.GraveTeleportEvent;
import dev.cwhead.GravesXAddon.LandProtection;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.List;

public class LandProtectionWorldGuardGraveCreateListener implements Listener {

    private final LandProtection plugin;

    public LandProtectionWorldGuardGraveCreateListener(LandProtection plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onGraveCreate(GraveCreateEvent event) {
        Player player = (Player) event.getEntity();
        Location deathLocation = player != null ? player.getLocation() : null;

        if (player == null) {
            return;
        }

        boolean isMember = plugin.getWorldGuard().canCreateGrave(player, deathLocation);
        String canCreate = isMember ? "can" : "can't";
        plugin.getGravesXAPI().getGravesX().debugMessage(player.getDisplayName() + " " + canCreate + " create a grave in worldguard region location: " + deathLocation, 2);

        List<String> regionKeys = plugin.getWorldGuard().getRegionKeyList(deathLocation);

        for (String regionKey : regionKeys) {
            String regionId = regionKey.split("\\|")[2];
            if (plugin.getWorldGuard().isMember(regionId, player)) {
                isMember = true;
                break;
            }
        }

        if (!isMember) {
            player.sendMessage(ChatColor.GRAY + "☠ " + ChatColor.RED + "You must be a member of the region or have permission to create a grave here.");
            plugin.getGravesXAPI().getGravesX().debugMessage(player.getDisplayName() + " isn't a member of a worldguard region location: " + deathLocation, 2);
            event.setAddon(true);
            event.setCancelled(true);
        } else {
            plugin.getGravesXAPI().getGravesX().debugMessage(player.getDisplayName() + " is a member of a worldguard region location: " + deathLocation, 2);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onGraveTeleport(GraveTeleportEvent event) {
        Player player = event.getPlayer();
        Location deathLocation = player != null ? player.getLocation() : null;

        if (player == null) {
            return;
        }

        boolean isMember = plugin.getWorldGuard().canTeleport(player, deathLocation);
        String canTeleport = isMember ? "can" : "can't";
        plugin.getGravesXAPI().getGravesX().debugMessage(player.getDisplayName() + " " + canTeleport + " teleport to a grave in worldguard region location: " + deathLocation, 2);

        List<String> regionKeys = plugin.getWorldGuard().getRegionKeyList(deathLocation);

        for (String regionKey : regionKeys) {
            String regionId = regionKey.split("\\|")[2];
            if (plugin.getWorldGuard().isMember(regionId, player)) {
                isMember = true;
                break;
            }
        }

        if (!isMember) {
            player.sendMessage(ChatColor.GRAY + "☠ " + ChatColor.RED + "You must be a member of the region or have permission to teleport to your grave in this region.");
            plugin.getGravesXAPI().getGravesX().debugMessage(player.getDisplayName() + " isn't a member of a worldguard region location: " + deathLocation, 2);
            event.setAddon(true);
            event.setCancelled(true);
        } else {
            plugin.getGravesXAPI().getGravesX().debugMessage(player.getDisplayName() + " is a member of a worldguard region location: " + deathLocation, 2);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onGraveOpen(GraveOpenEvent event) {
        Player player = event.getPlayer();
        Location deathLocation = player != null ? player.getLocation() : null;

        if (player == null) {
            return;
        }

        boolean isMember = plugin.getWorldGuard().canLoot(player, deathLocation);
        String canOpen = isMember ? "can" : "can't";
        plugin.getGravesXAPI().getGravesX().debugMessage(player.getDisplayName() + " " + canOpen + " open a grave in worldguard region location: " + deathLocation, 2);

        List<String> regionKeys = plugin.getWorldGuard().getRegionKeyList(deathLocation);

        for (String regionKey : regionKeys) {
            String regionId = regionKey.split("\\|")[2];
            if (plugin.getWorldGuard().isMember(regionId, player)) {
                isMember = true;
                break;
            }
        }

        if (!isMember) {
            player.sendMessage(ChatColor.GRAY + "☠ " + ChatColor.RED + "You must be a member of the region or have permission to open a grave in this region.");
            plugin.getGravesXAPI().getGravesX().debugMessage(player.getDisplayName() + " isn't a member of a worldguard region location: " + deathLocation, 2);
            event.setAddon(true);
            event.setCancelled(true);
        } else {
            plugin.getGravesXAPI().getGravesX().debugMessage(player.getDisplayName() + " is a member of a worldguard region location: " + deathLocation, 2);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onGraveAutoLooted(GraveAutoLootEvent event) {
        Player player = event.getPlayer();
        Location deathLocation = player != null ? player.getLocation() : null;

        if (player == null) {
            return;
        }

        boolean isMember = plugin.getWorldGuard().canAutoLoot(player, deathLocation);
        String canAutoLoot = isMember ? "can" : "can't";
        plugin.getGravesXAPI().getGravesX().debugMessage(player.getDisplayName() + " " + canAutoLoot + " auto loot a grave in worldguard region location: " + deathLocation, 2);

        List<String> regionKeys = plugin.getWorldGuard().getRegionKeyList(deathLocation);

        for (String regionKey : regionKeys) {
            String regionId = regionKey.split("\\|")[2];
            if (plugin.getWorldGuard().isMember(regionId, player)) {
                isMember = true;
                break;
            }
        }

        if (!isMember) {
            player.sendMessage(ChatColor.GRAY + "☠ " + ChatColor.RED + "You must be a member of the region or have permission to auto loot a grave in this region.");
            plugin.getGravesXAPI().getGravesX().debugMessage(player.getDisplayName() + " isn't a member of a worldguard region location: " + deathLocation, 2);
            event.setAddon(true);
            event.setCancelled(true);
        } else {
            plugin.getGravesXAPI().getGravesX().debugMessage(player.getDisplayName() + " is a member of a worldguard region location: " + deathLocation, 2);
        }
    }
}