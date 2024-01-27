package me.champion.manhuntplugin;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class PlayerJoinListener implements Listener {

    private final MhStart mhStart;

    public PlayerJoinListener(MhStart mhStart) {
        this.mhStart = mhStart;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Check if the game has not started
        if (!mhStart.isGameStarted()) {
            // Get the spawn location and teleport the player
            Location spawnLocation = MhCreate.getSpawnLocation();
            if (spawnLocation != null) {
                player.teleport(spawnLocation);
            }
        }
    }
}

