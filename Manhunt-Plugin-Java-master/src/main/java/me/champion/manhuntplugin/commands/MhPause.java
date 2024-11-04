package me.champion.manhuntplugin.commands;

import me.champion.manhuntplugin.Manhunt;
import me.champion.manhuntplugin.TeamManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MhPause implements CommandExecutor {

    private final TeamManager teamManager;
    private final Manhunt plugin; // Reference to the Manhunt plugin

    public MhPause(Manhunt plugin, TeamManager teamManager) {
        this.plugin = plugin;
        this.teamManager = teamManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check if the sender is a player with OP permissions
        if (sender instanceof Player && sender.isOp()) {
            Player player = (Player) sender;

            // Toggle the game's pause state
            if (!teamManager.isGamePaused()) {
                // If the game is not paused, pause it
                teamManager.pauseGame(player);
            } else {
                // If the game is already paused, notify the sender
                sender.sendMessage("§cGame is already paused.");
            }
            return true;
        } else {
            // Sender does not have OP permissions
            sender.sendMessage("§cYou do not have OP");
            return true;
        }
    }
}