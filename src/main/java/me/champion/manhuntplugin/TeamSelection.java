package me.champion.manhuntplugin;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class TeamSelection implements Listener {

    private final TeamManager teamManager;
    private final MhStart mhStart;

    public TeamSelection(Manhunt plugin, TeamManager teamManager, MhStart mhStart) {
        this.teamManager = teamManager;
        this.mhStart = mhStart;

        // Register the listener
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (mhStart.isGameStarted()) {
            return; // Game has started, do not change teams
        }

        Material blockType = event.getPlayer().getLocation().subtract(0, 1, 0).getBlock().getType();

        // Check if the player stepped on a concrete block
        if (blockType == Material.RED_CONCRETE || blockType == Material.LIGHT_BLUE_CONCRETE) {
            addToTeam(event.getPlayer(), blockType);
        }
    }

    private void addToTeam(Player player, Material blockType) {
        String team;

        if (blockType == Material.RED_CONCRETE) {
            team = "Zombies";
        } else if (blockType == Material.LIGHT_BLUE_CONCRETE) {
            team = "Runners";
        } else {
            return; // If it's neither red nor light blue concrete, do nothing
        }

        // Check if the player is already on this team
        if (teamManager.isOnTeam(player, team)) {
            return; // Player is already on this team, do nothing
        }

        teamManager.addToTeam(player, team);
    }
}