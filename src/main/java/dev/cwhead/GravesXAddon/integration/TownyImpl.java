package dev.cwhead.GravesXAddon.integration;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import dev.cwhead.GravesXAddon.LandProtection;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * The {@code TownyImpl} class is responsible for interacting with Towny's town and nation systems
 * to determine if specific actions can be performed on grave-related entities and locations.
 * It handles permission checks for actions such as grave creation, teleportation,
 * looting, and autolooting, as well as determining town membership for players.
 */
public class TownyImpl {
    private final LandProtection plugin;
    private final TownyAPI townyAPI;

    /**
     * Constructs a {@code TownyImpl} object.
     *
     * @param plugin the {@code LandProtection} plugin instance
     */
    public TownyImpl(LandProtection plugin) {
        this.plugin = plugin;
        this.townyAPI = TownyAPI.getInstance();
    }

    /**
     * Checks if the specified player is a resident.
     *
     * @param entity the entity to check (must be a player)
     * @param location the location to check
     * @return {@code true} if the player is a resident at the location of death, {@code false} otherwise
     */
    public boolean isResident(Entity entity, Location location) {
        if (!(entity instanceof Player)) {
            return true;
        }

        Player player = (Player) entity;
        TownBlock townBlock = townyAPI.getTownBlock(location);

        // Allow grave creation if location is not in a town
        if (townBlock == null || !townBlock.hasTown()) {
            return true;
        }

        Town town = townBlock.getTownOrNull();
        Resident resident = townyAPI.getResident(player);

        // Check if the resident is part of the town and has permissions
        return resident != null && town.hasResident(resident);
    }
}