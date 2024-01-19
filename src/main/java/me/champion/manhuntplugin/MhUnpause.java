package me.champion.manhuntplugin;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MhUnpause implements CommandExecutor {

    private final TeamManager teamManager;

    public MhUnpause(TeamManager teamManager) {
        this.teamManager = teamManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player && sender.isOp()) {
            Player player = (Player) sender;

            // Check if the game is already unpaused
            if (!teamManager.isGamePaused()) {
                player.sendMessage("The game is not paused.");
                return true;
            }

            // Unpause the game
            teamManager.unpauseGame(player);

            // Reset the player's walk speed to the default Minecraft value (0.2)
            player.setWalkSpeed(0.2f);

            // Reset walk speed for all online players
            resetWalkSpeedForAllPlayers();

            // Switch the player's game mode back to Survival
            player.setGameMode(GameMode.SURVIVAL);

            // Execute /tick unfreeze command
            Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "tick unfreeze");

            return true;
        } else {
            sender.sendMessage("You do not have permission to use this command.");
            return true;
        }
    }

    // Method to reset walk speed for all online players
    private void resetWalkSpeedForAllPlayers() {
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            onlinePlayer.setWalkSpeed(0.2f); // Reset walk speed for each player
        }
    }
}