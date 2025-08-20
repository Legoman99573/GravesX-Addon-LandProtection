package dev.cwhead.GravesXAddon.integration;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import dev.cwhead.GravesXAddon.LandProtection;
import dev.cwhead.GravesXAddon.config.LandProtectionTownyConfig;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Objects;
import java.util.Set;

/**
 * Towny integration driven by /plugins/GravesX/Addon/Land-Protection/towny.yml
 */
public class TownyImpl {

    private final LandProtection plugin;

    private final TownyAPI towny;
    private final LandProtectionTownyConfig config;

    private Set<String> allyAllowedTypes;
    private Set<String> outsiderAllowedTypes;
    private boolean wildernessAllowed;
    private boolean residentsAlwaysAllowed;

    public TownyImpl(LandProtection plugin) {
        this.plugin = plugin;
        this.towny = TownyAPI.getInstance();
        this.config = new LandProtectionTownyConfig();
        loadFromConfig();
    }

    public void reloadConfig() {
        config.reload();
        loadFromConfig();
    }

    private void loadFromConfig() {
        this.allyAllowedTypes = config.getAllyAllowedTypes();
        this.outsiderAllowedTypes = config.getOutsiderAllowedTypes();
        this.wildernessAllowed = config.isWildernessAllowed();
        this.residentsAlwaysAllowed = config.isResidentsAlwaysAllowed();
    }

    public boolean canCreateGrave(Player p, Location loc) {
        return allowedByTowny(p, loc);
    }

    public boolean canTeleport(Player p, Location loc) {
        return allowedByTowny(p, loc);
    }

    public boolean canLoot(Player p, Location loc) {
        return allowedByTowny(p, loc);
    }

    public boolean canAutoLoot(Player p, Location loc) {
        return allowedByTowny(p, loc);
    }

    public boolean canWalkOver(Player p, Location loc) {
        return allowedByTowny(p, loc);
    }

    public boolean canProjectile(Player p, Location loc) {
        return allowedByTowny(p, loc);
    }

    public boolean canBreak(Player p, Location loc) {
        return allowedByTowny(p, loc);
    }

    public boolean isResident(Player p, Location loc) {
        return (residentsAlwaysAllowed && isResidentOfBlockTown(p, loc)) || (wildernessAllowed && isWilderness(loc));
    }

    private boolean allowedByTowny(Player player, Location loc) {
        if (player == null) return true;

        if (isWilderness(loc)) return wildernessAllowed;

        TownBlock block = towny.getTownBlock(loc);
        if (block == null || !block.hasTown()) return wildernessAllowed;

        Town plotTown = block.getTownOrNull();
        if (plotTown == null) return wildernessAllowed;

        if (residentsAlwaysAllowed && isResidentOf(player, plotTown)) return true;

        final String type = getTypeId(block);

        if (isAllyOf(player, plotTown) && type != null && allyAllowedTypes.contains(type)) return true;

        if (!isAllyOf(player, plotTown) && type != null && outsiderAllowedTypes.contains(type)) return true;

        return false;
    }

    private boolean isWilderness(Location loc) {
        if (loc == null || loc.getWorld() == null) return true;
        TownBlock block = towny.getTownBlock(loc);
        return block == null || !block.hasTown();
    }

    private boolean isResidentOfBlockTown(Player p, Location loc) {
        TownBlock block = towny.getTownBlock(loc);
        Town town = (block != null) ? block.getTownOrNull() : null;
        return town != null && isResidentOf(p, town);
    }

    private boolean isResidentOf(Player p, Town town) {
        if (p == null || town == null) return false;
        Resident res = towny.getResident(p);
        return res != null && town.hasResident(res);
    }

    private boolean isAllyOf(Player p, Town plotTown) {
        if (p == null || plotTown == null) return false;

        Resident res = towny.getResident(p);
        Town playerTown = (res != null) ? res.getTownOrNull() : null;
        if (playerTown == null) return false;

        var plotNation = plotTown.getNationOrNull();
        var playerNation = playerTown.getNationOrNull();
        if (plotNation == null || playerNation == null) return false;

        return plotNation.hasAlly(playerNation) || playerNation.hasAlly(plotNation);
    }

    private String getTypeId(TownBlock block) {
        try {
            Object type = (block == null) ? null : block.getType();
            if (type == null) return null;
            return Objects.toString(type.getClass().getMethod("name").invoke(type), null);
        } catch (Throwable ignored) {
            return null;
        }
    }

    public LandProtectionTownyConfig getConfig() {
        return config;
    }
}