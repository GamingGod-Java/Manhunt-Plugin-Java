package me.champion.manhuntplugin;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPortalEvent;

public class WinCondition implements Listener {
    private final TeamManager teamManager;
    public WinCondition(TeamManager teamManager) {
        this.teamManager = teamManager;
    }
    public static boolean endEntered = false;
    @EventHandler
    public void onPlayerPortal(PlayerPortalEvent event) {
        Player player = event.getPlayer();

        // Check if the player is a runner and has entered the End portal
        if (teamManager.isOnTeam(player, "Runners") && event.getTo().getWorld().getEnvironment() == World.Environment.THE_END) {
            endEntered = true;  // Set the flag to true
        }
    }
}