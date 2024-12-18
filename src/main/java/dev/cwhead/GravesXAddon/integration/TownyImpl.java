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
     * Checks if the specified player has permission to create a grave in the town at the specified location.
     *
     * @param entity the entity to check (must be a player)
     * @param location the location where the grave would be created
     * @return {@code true} if the player is allowed to create a grave at the location, {@code false} otherwise
     */
    public boolean canCreateGrave(Entity entity, Location location) {
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

    /**
     * Checks if the specified player has permission to teleport to the location within Towny.
     *
     * @param entity the entity to check (must be a player)
     * @param location the location where teleportation is being attempted
     * @return {@code true} if the player is allowed to teleport at the location, {@code false} otherwise
     */
    public boolean canTeleport(Entity entity, Location location) {
        if (!(entity instanceof Player)) {
            return true;
        }

        Player player = (Player) entity;
        TownBlock townBlock = townyAPI.getTownBlock(location);

        // Allow teleportation if location is not in a town
        if (townBlock == null || !townBlock.hasTown()) {
            return true;
        }

        Town town = townBlock.getTownOrNull();
        Resident resident = townyAPI.getResident(player);

        // Check if the resident is allowed to teleport
        return resident != null && town.hasResident(resident);
    }

    /**
     * Checks if the specified player has permission to loot a grave at the location within Towny.
     *
     * @param entity the entity to check (must be a player)
     * @param location the location of the grave
     * @return {@code true} if the player is allowed to loot the grave at the location, {@code false} otherwise
     */
    public boolean canLoot(Entity entity, Location location) {
        if (!(entity instanceof Player)) {
            return true;
        }

        Player player = (Player) entity;
        TownBlock townBlock = townyAPI.getTownBlock(location);

        if (townBlock == null || !townBlock.hasTown()) {
            return true;
        }

        Town town = townBlock.getTownOrNull();
        Resident resident = townyAPI.getResident(player);

        return resident != null && town.hasResident(resident);
    }

    /**
     * Checks if the specified player has permission to autoloot a grave at the location within Towny.
     *
     * @param entity the entity to check (must be a player)
     * @param location the location of the grave
     * @return {@code true} if the player is allowed to autoloot the grave at the location, {@code false} otherwise
     */
    public boolean canAutoLoot(Entity entity, Location location) {
        if (!(entity instanceof Player)) {
            return true;
        }

        Player player = (Player) entity;
        TownBlock townBlock = townyAPI.getTownBlock(location);

        if (townBlock == null || !townBlock.hasTown()) {
            return true;
        }

        Town town = townBlock.getTownOrNull();
        Resident resident = townyAPI.getResident(player);

        return resident != null && town.hasResident(resident);
    }

    /**
     * Checks if the specified player has permission to use a projectile on a grave at the location within Towny.
     *
     * @param entity the entity to check (must be a player)
     * @param location the location of the grave
     * @return {@code true} if the player is allowed to projectile destroy the grave at the location, {@code false} otherwise
     */
    public boolean canProjectile(Entity entity, Location location) {
        if (!(entity instanceof Player)) {
            return true;
        }

        Player player = (Player) entity;
        TownBlock townBlock = townyAPI.getTownBlock(location);

        if (townBlock == null || !townBlock.hasTown()) {
            return true;
        }

        Town town = townBlock.getTownOrNull();
        Resident resident = townyAPI.getResident(player);

        return resident != null && town.hasResident(resident);
    }

    /**
     * Checks if the specified player is a member of the specified town.
     *
     * @param townName the name of the town
     * @param player the player to check
     * @return {@code true} if the player is a member of the town, {@code false} otherwise
     */
    public boolean isMember(String townName, Player player) {
        Resident resident = townyAPI.getResident(player);
        Town town = townyAPI.getTown(townName);

        return resident != null && town != null && town.hasResident(resident);
    }

    /**
     * Retrieves a list of town keys that apply to the specified location.
     *
     * @param location the location to check for applicable towns
     * @return a list of town keys in the format "towny|<worldName>|<townName>"
     */
    public List<String> getTownKeyList(Location location) {
        List<String> townKeyList = new ArrayList<>();

        if (location.getWorld() != null) {
            TownBlock townBlock = townyAPI.getTownBlock(location);

            if (townBlock != null && townBlock.hasTown()) {
                Town town = townBlock.getTownOrNull();
                if (town != null) {
                    townKeyList.add("towny|" + location.getWorld().getName() + "|" + town.getName());
                }
            }
        }

        return townKeyList;
    }
}