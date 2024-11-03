package me.champion.manhuntplugin.listeners;

import me.champion.manhuntplugin.TeamManager;
import me.champion.manhuntplugin.commands.MhStart;
import me.champion.manhuntplugin.Manhunt;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {

    private final MhStart mhStart;
    private final TeamManager teamManager;

    public PlayerJoinListener(MhStart mhStart, TeamManager teamManager) {
        this.mhStart = mhStart;
        this.teamManager = teamManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Log the player joining
        Bukkit.getLogger().info("Player " + player.getName() + " joined the game.");

        // Check if the game has started
        if (mhStart.isGameStarted()) {
            // Game has started: set player to Spectator mode
            player.setGameMode(GameMode.SPECTATOR);
            Bukkit.getLogger().info("Game has started. Player " + player.getName() + " set to Spectator mode.");

            // Add player to the boss bar if it exists and is visible
            BossBar bossBar = mhStart.getBossBar();
            if (bossBar != null && bossBar.isVisible()) {
                Bukkit.getScheduler().runTaskLater(Manhunt.getPlugin(), () -> {
                    bossBar.addPlayer(player);
                    Bukkit.getLogger().info("Player " + player.getName() + " added to the boss bar.");
                }, 20L); // 1-second delay
            }
        } else {
            // Game has not started: set player to Adventure mode
            player.setGameMode(GameMode.ADVENTURE);
            Bukkit.getLogger().info("Game has not started. Player " + player.getName() + " set to Adventure mode.");

            // Teleport the player to the world_spawn using the /mhswap command
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mhswap " + player.getName() + " world_spawn");
            Bukkit.getLogger().info("Player " + player.getName() + " teleported to world_spawn.");
        }
    }
}
