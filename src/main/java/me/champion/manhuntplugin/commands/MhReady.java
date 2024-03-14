package me.champion.manhuntplugin.commands;

import me.champion.manhuntplugin.TeamManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class MhReady implements CommandExecutor, Listener {

    private final TeamManager teamManager;
    private final Plugin plugin;
    private boolean isUnpausing = false; // Variable to track if unpausing is in progress

    public MhReady(TeamManager teamManager, Plugin plugin) {
        this.teamManager = teamManager;
        this.plugin = plugin;

        // Register the listener
        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player && sender.isOp()) {
            Player player = (Player) sender;

            // Check if unpausing is already in progress
            if (isUnpausing) {
                player.sendMessage(ChatColor.RED + "The game is already unpausing. Please wait.");
                return true;
            }

            // Check if the game is already unpaused
            if (!teamManager.isGamePaused()) {
                player.sendMessage(ChatColor.RED + "The game is already unpaused.");
                return true;
            }

            // Set isUnpausing to true to prevent spamming during the countdown
            isUnpausing = true;

            // Display countdown messages with purple text
            sendCountdownMessages(3);

            // Delay the unpause action
            new BukkitRunnable() {
                @Override
                public void run() {
                    // Check if the game is still paused (prevent race condition)
                    if (teamManager.isGamePaused()) {
                        // Unpause the game using teamManager
                        teamManager.unpauseGame(player);
                    }

                    // Reset isUnpausing to allow the command again
                    isUnpausing = false;
                }
            }.runTaskLater(plugin, 60L); // 60L represents 3 seconds (20 ticks per second)

            return true;
        } else {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true; // Cancel the chat event to prevent "/MhReady" from being displayed
        }
    }

    private void sendCountdownMessages(int seconds) {
        new BukkitRunnable() {
            int count = seconds;

            @Override
            public void run() {
                if (count > 1) {
                    Bukkit.broadcastMessage(ChatColor.LIGHT_PURPLE + "Game unpausing in " + count + " seconds...");
                } else if (count == 1) {
                    Bukkit.broadcastMessage(ChatColor.LIGHT_PURPLE + "Game unpausing in " + count + " second...");
                } else {
                    cancel(); // Stop the countdown
                    return;  // Added to exit the method after canceling
                }

                count--; // Move count-- inside the "if" and "else if" blocks
            }
        }.runTaskTimer(plugin, 0L, 20L); // 20L represents 1 second (20 ticks per second)
    }

    @EventHandler
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        // Cancel the chat event if the command matches /MhReady to prevent it from being displayed
        if (event.getMessage().equalsIgnoreCase("/MhReady") && !event.getPlayer().isOp()) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "You do not have permission to use this command.");
        }
    }
}