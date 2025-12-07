package dev.cwhead.GravesXAddon.listener.dependencies;

import com.ranull.graves.type.Grave;
import dev.cwhead.GravesXAddon.LandProtection;
import me.ryanhamshire.GriefPrevention.events.ClaimPermissionCheckEvent;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class GriefPreventionListener implements Listener {

    private final LandProtection plugin;

    public GriefPreventionListener(LandProtection plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onGPCheck(ClaimPermissionCheckEvent e) {
        Player p = e.getCheckedPlayer();
        if (p == null) return;
        Location location = p.getLocation();

        Grave grave = plugin.getGravesXAPI().plugin().getBlockManager().getGraveFromBlock(location.getBlock());

        if (grave == null) return;

        if (location == grave.getLocationDeath()) {
            e.setDenialReason(null);
        }
    }
}