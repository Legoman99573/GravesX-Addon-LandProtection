package dev.cwhead.GravesXAddon.commands;

import dev.cwhead.GravesXAddon.LandProtection;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class LandProtectionCommand implements CommandExecutor, TabCompleter {

    private static final String PERM = "gravesx.landprotection.commands";
    private static final String DEBUG = "gravesx.landprotection.debug";
    private final LandProtection plugin;

    public LandProtectionCommand(LandProtection plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             @NotNull String[] args) {
        if (args.length == 0 || !"reload".equalsIgnoreCase(args[0])) {
            if ((sender instanceof ConsoleCommandSender) && plugin.getGravesXAPI().getGravesX().hasGrantedPermission(DEBUG, (Player) sender)) {
                sender.sendMessage(ChatColor.RED + "GravesXAddon-LandProtection Debug Information");
                sender.sendMessage(ChatColor.RED + "GriefPrevention: " + (plugin.isGriefPreventionEnabled() ? ChatColor.GREEN + "listening" : ChatColor.RED + "not listening"));
                sender.sendMessage(ChatColor.RED + "GriefDefender: " + (plugin.isGriefDefenderEnabled() ? ChatColor.GREEN + "listening" : ChatColor.RED + "not listening"));
                sender.sendMessage(ChatColor.RED + "Lands: " + (plugin.isLandsEnabled() ? ChatColor.GREEN + "listening" : ChatColor.RED + "not listening"));
                sender.sendMessage(ChatColor.RED + "Towny: " + (plugin.isTownyEnabled() ? ChatColor.GREEN + "listening" : ChatColor.RED + "not listening"));
                sender.sendMessage(ChatColor.RED + "WorldGuard: " + (plugin.isWorldGuardEnabled() ? ChatColor.GREEN + "listening" : ChatColor.RED + "not listening"));
                return true;
            }
            sender.sendMessage(ChatColor.RED + "You don't have permission to do that.");
            return true;
        }

        if (!(sender instanceof ConsoleCommandSender) && !plugin.getGravesXAPI().getGravesX().hasGrantedPermission(PERM, (Player) sender)) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to do that.");
            return true;
        }

        try {
            plugin.reloadAllConfigs();
        } catch (Throwable ignored) {
            //ignored
        }

        sender.sendMessage(ChatColor.GREEN + "GravesX Land Protection: configs reloaded.");
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender,
                                                @NotNull Command command,
                                                @NotNull String alias,
                                                @NotNull String[] args) {
        if (args.length == 1) {
            if (!(sender instanceof ConsoleCommandSender) && !plugin.getGravesXAPI().getGravesX().hasGrantedPermission(PERM, (Player) sender))
                return Collections.emptyList();

            if ("reload".startsWith(args[0].toLowerCase()))
                return Collections.singletonList("reload");
        }
        return Collections.emptyList();
    }
}