package me.champion.manhuntplugin.listeners;

import me.champion.manhuntplugin.commands.MhCreate;
import me.champion.manhuntplugin.commands.MhStart;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.boss.BossBar;

public class PlayerJoinListener implements Listener {

    private final MhStart mhStart;

    public PlayerJoinListener(MhStart mhStart) {
        this.mhStart = mhStart;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Check if the game has already started
        if (mhStart.isGameStarted()) {
            // Get the boss bar from MhStart
            BossBar bossBar = mhStart.getBossBar();

            // If the boss bar exists and is visible, add the player to the boss bar
            if (bossBar != null && bossBar.isVisible()) {
                bossBar.addPlayer(player);
            }
        } else {
            // The game has not started, handle the player joining as per your existing logic
            Location spawnLocation = MhCreate.getSpawnLocation();
            if (spawnLocation != null) {
                player.teleport(spawnLocation);
            }
        }
    }
}
