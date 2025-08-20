package dev.cwhead.GravesXAddon.commands;

import dev.cwhead.GravesXAddon.LandProtection;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class LandProtectionCommand implements CommandExecutor, TabCompleter {

    private static final String PERM  = "gravesx.landprotection.commands";
    private static final String DEBUG = "gravesx.landprotection.debug";

    private final LandProtection plugin;

    public LandProtectionCommand(LandProtection plugin) {
        this.plugin = plugin;
    }

    private boolean hasPerm(CommandSender sender, String perm) {
        if (sender instanceof Player) {
            Player p = ((Player) sender);
            return plugin.getGravesXAPI().getGravesX().hasGrantedPermission(perm, p);
        }
        return true;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             @NotNull String[] args) {

        if (args.length == 0 || !"reload".equalsIgnoreCase(args[0])) {
            if (!hasPerm(sender, DEBUG)) {
                sender.sendMessage(ChatColor.RED + "☠ You don't have permission to do that.");
                return true;
            }

            sender.sendMessage(ChatColor.RED + "☠ " + ChatColor.GOLD + "GravesXAddon-LandProtection Debug Information");
            sender.sendMessage(ChatColor.GOLD + "GriefPrevention: " + (plugin.isGriefPreventionEnabled() ? ChatColor.GREEN + "Hooked" : ChatColor.RED + "Not Hooked"));
            sender.sendMessage(ChatColor.GOLD + "GriefDefender: " + (plugin.isGriefDefenderEnabled() ? ChatColor.GREEN + "Hooked" : ChatColor.RED + "Not Hooked"));
            sender.sendMessage(ChatColor.GOLD + "Lands: " + (plugin.isLandsEnabled() ? ChatColor.GREEN + "Hooked" : ChatColor.RED + "Not Hooked"));
            sender.sendMessage(ChatColor.GOLD + "Towny: " + (plugin.isTownyEnabled() ? ChatColor.GREEN + "Hooked" : ChatColor.RED + "Not Hooked"));
            sender.sendMessage(ChatColor.GOLD + "WorldGuard: " + (plugin.isWorldGuardEnabled() ? ChatColor.GREEN + "Hooked" : ChatColor.RED + "Not Hooked"));
            return true;
        }

        if (!hasPerm(sender, PERM)) {
            sender.sendMessage(ChatColor.RED + "☠ You don't have permission to do that.");
            return true;
        }

        try {
            plugin.reloadAllConfigs();
        } catch (Throwable ignored) {
            // ignored
        }

        sender.sendMessage(ChatColor.RED + "☠ " + ChatColor.GREEN + "GravesX Land Protection: configs reloaded.");
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender,
                                                @NotNull Command command,
                                                @NotNull String alias,
                                                @NotNull String[] args) {
        if (args.length == 1) {
            if (hasPerm(sender, PERM)) {
                String partial = args[0].toLowerCase();
                if ("reload".startsWith(partial)) {
                    return Collections.singletonList("reload");
                }
            }
        }
        return Collections.emptyList();
    }
}