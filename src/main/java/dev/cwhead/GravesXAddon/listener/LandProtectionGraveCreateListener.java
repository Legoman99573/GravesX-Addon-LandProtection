package dev.cwhead.GravesXAddon.listener;

import com.ranull.graves.event.*;
import dev.cwhead.GravesXAddon.LandProtection;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.List;

/**
 * Listener for handling events related to grave interactions with region checks.
 *
 * This listener intercepts events related to grave creation, teleportation, opening, and auto-looting
 * and ensures that the player is a member of the respective WorldGuard region or has the necessary permissions.
 * It integrates with the LandProtection plugin to enforce these rules.
 */
public class LandProtectionGraveCreateListener implements Listener {

    private final LandProtection plugin;

    /**
     * Constructor for the listener. Initializes the plugin instance.
     *
     * @param plugin The LandProtection plugin instance.
     */
    public LandProtectionGraveCreateListener(LandProtection plugin) {
        this.plugin = plugin;
    }

    /**
     * Handles the event of creating a grave. Checks if the player is allowed to create a grave in the
     * WorldGuard region where the grave is being placed, as well as Towny permissions.
     *
     * @param event The GraveCreateEvent that contains information about the player and the grave location.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onGraveCreate(GraveCreateEvent event) {
        Player player = (Player) event.getEntity();
        Location deathLocation = player != null ? player.getLocation() : null;

        if (player == null) {
            return;
        }

        boolean isWorldGuardEnabled = plugin.isWorldGuardEnabled();
        boolean isTownyEnabled = plugin.isTownyEnabled();

        boolean isWorldGuardMember = true;
        if (isWorldGuardEnabled) {
            isWorldGuardMember = plugin.getWorldGuard().canCreateGrave(player, deathLocation);
            List<String> regionKeys = plugin.getWorldGuard().getRegionKeyList(deathLocation);
            for (String regionKey : regionKeys) {
                String regionId = regionKey.split("\\|")[2];
                if (plugin.getWorldGuard().isMember(regionId, player)) {
                    isWorldGuardMember = true;
                    break;
                }
            }
        }

        boolean isTownyMember = true;
        if (isTownyEnabled) {
            isTownyMember = plugin.getTowny().canCreateGrave(player, deathLocation);
        }

        if (!isWorldGuardMember || !isTownyMember) {
            player.sendMessage(ChatColor.GRAY + "☠ " + ChatColor.RED + "You must be a member of the region or have permission to create a grave here.");
            event.setAddon(true);
            event.setCancelled(true);
        } else {
            plugin.getGravesXAPI().getGravesX().debugMessage(player.getDisplayName() + " can create a grave at: " + deathLocation, 2);
        }
    }

    /**
     * Handles the event of teleporting to a grave. Checks if the player is allowed to teleport to the grave
     * within the WorldGuard region where the grave is located.
     *
     * @param event The GraveTeleportEvent that contains information about the player and the grave location.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onGraveTeleport(GraveTeleportEvent event) {
        Player player = event.getPlayer();
        Location deathLocation = player != null ? player.getLocation() : null;

        if (player == null) {
            return;
        }

        boolean isWorldGuardEnabled = plugin.isWorldGuardEnabled();
        boolean isTownyEnabled = plugin.isTownyEnabled();

        boolean isWorldGuardMember = true;
        if (isWorldGuardEnabled) {
            isWorldGuardMember = plugin.getWorldGuard().canTeleport(player, deathLocation);
            List<String> regionKeys = plugin.getWorldGuard().getRegionKeyList(deathLocation);
            for (String regionKey : regionKeys) {
                String regionId = regionKey.split("\\|")[2];
                if (plugin.getWorldGuard().isMember(regionId, player)) {
                    isWorldGuardMember = true;
                    break;
                }
            }
        }

        boolean isTownyMember = true;
        if (isTownyEnabled) {
            isTownyMember = plugin.getTowny().canTeleport(player, deathLocation);
        }

        if (!isWorldGuardMember || !isTownyMember) {
            player.sendMessage(ChatColor.GRAY + "☠ " + ChatColor.RED + "You must be a member of the region or have permission to teleport to your grave in this region.");
            event.setAddon(true);
            event.setCancelled(true);
        } else {
            plugin.getGravesXAPI().getGravesX().debugMessage(player.getDisplayName() + " can teleport to grave at: " + deathLocation, 2);
        }
    }

    /**
     * Handles the event of opening a grave. Checks if the player is allowed to open a grave within the
     * WorldGuard region where the grave is located.
     *
     * @param event The GraveOpenEvent that contains information about the player and the grave location.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onGraveOpen(GraveOpenEvent event) {
        Player player = event.getPlayer();
        Location deathLocation = player != null ? player.getLocation() : null;

        if (player == null) {
            return;
        }

        boolean isWorldGuardEnabled = plugin.isWorldGuardEnabled();
        boolean isTownyEnabled = plugin.isTownyEnabled();

        boolean isWorldGuardMember = true;
        if (isWorldGuardEnabled) {
            isWorldGuardMember = plugin.getWorldGuard().canLoot(player, deathLocation);
            List<String> regionKeys = plugin.getWorldGuard().getRegionKeyList(deathLocation);
            for (String regionKey : regionKeys) {
                String regionId = regionKey.split("\\|")[2];
                if (plugin.getWorldGuard().isMember(regionId, player)) {
                    isWorldGuardMember = true;
                    break;
                }
            }
        }

        boolean isTownyMember = true;
        if (isTownyEnabled) {
            isTownyMember = plugin.getTowny().canLoot(player, deathLocation);
        }

        if (!isWorldGuardMember || !isTownyMember) {
            player.sendMessage(ChatColor.GRAY + "☠ " + ChatColor.RED + "You must be a member of the region or have permission to open a grave in this region.");
            event.setAddon(true);
            event.setCancelled(true);
        } else {
            plugin.getGravesXAPI().getGravesX().debugMessage(player.getDisplayName() + " can open grave at: " + deathLocation, 2);
        }
    }

    /**
     * Handles the event of automatically looting a grave. Checks if the player is allowed to auto loot a grave
     * within the WorldGuard region where the grave is located.
     *
     * @param event The GraveAutoLootEvent that contains information about the player and the grave location.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onGraveAutoLooted(GraveAutoLootEvent event) {
        Player player = event.getPlayer();
        Location deathLocation = player != null ? player.getLocation() : null;

        if (player == null) {
            return;
        }

        boolean isWorldGuardEnabled = plugin.isWorldGuardEnabled();
        boolean isTownyEnabled = plugin.isTownyEnabled();

        boolean isWorldGuardMember = true;
        if (isWorldGuardEnabled) {
            isWorldGuardMember = plugin.getWorldGuard().canAutoLoot(player, deathLocation);
            List<String> regionKeys = plugin.getWorldGuard().getRegionKeyList(deathLocation);
            for (String regionKey : regionKeys) {
                String regionId = regionKey.split("\\|")[2];
                if (plugin.getWorldGuard().isMember(regionId, player)) {
                    isWorldGuardMember = true;
                    break;
                }
            }
        }

        boolean isTownyMember = true;
        if (isTownyEnabled) {
            isTownyMember = plugin.getTowny().canAutoLoot(player, deathLocation);
        }

        if (!isWorldGuardMember || !isTownyMember) {
            player.sendMessage(ChatColor.GRAY + "☠ " + ChatColor.RED + "You must be a member of the region or have permission to auto loot a grave in this region.");
            event.setAddon(true);
            event.setCancelled(true);
        } else {
            plugin.getGravesXAPI().getGravesX().debugMessage(player.getDisplayName() + " can auto loot grave at: " + deathLocation, 2);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onGraveWalkedOver(GraveWalkOverEvent event) {
        Player player = event.getPlayer();
        Location deathLocation = player != null ? player.getLocation() : null;

        if (player == null) {
            return;
        }

        boolean isWorldGuardEnabled = plugin.isWorldGuardEnabled();
        boolean isTownyEnabled = plugin.isTownyEnabled();

        boolean isWorldGuardMember = true;
        if (isWorldGuardEnabled) {
            isWorldGuardMember = plugin.getWorldGuard().canWalkOver(player, deathLocation);
            List<String> regionKeys = plugin.getWorldGuard().getRegionKeyList(deathLocation);
            for (String regionKey : regionKeys) {
                String regionId = regionKey.split("\\|")[2];
                if (plugin.getWorldGuard().isMember(regionId, player)) {
                    isWorldGuardMember = true;
                    break;
                }
            }
        }

        boolean isTownyMember = true;
        if (isTownyEnabled) {
            isTownyMember = plugin.getTowny().canAutoLoot(player, deathLocation);
        }

        if (!isWorldGuardMember || !isTownyMember) {
            player.sendMessage(ChatColor.GRAY + "☠ " + ChatColor.RED + "You must be a member of the region or have permission to auto loot a grave in this region.");
            event.setAddon(true);
            event.setCancelled(true);
        } else {
            plugin.getGravesXAPI().getGravesX().debugMessage(player.getDisplayName() + " can auto loot grave at: " + deathLocation, 2);
        }
    }
}