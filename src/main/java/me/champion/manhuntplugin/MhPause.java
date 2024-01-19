package me.champion.manhuntplugin;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MhPause implements CommandExecutor {

    private final TeamManager teamManager;
    private final Manhunt plugin; // Add a reference to the Manhunt plugin
    public int continuousLookingTaskId = -1;

    public MhPause(Manhunt plugin, TeamManager teamManager) {
        this.plugin = plugin;
        this.teamManager = teamManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player && sender.isOp()) {
            if (!teamManager.isGamePaused()) {
                Player player = (Player) sender;

                // Call the pauseGame method in the TeamManager
                teamManager.pauseGame(player);

                // Set the player to Adventure mode
                player.setGameMode(GameMode.ADVENTURE);

                // Set the walk speed to 0 - this makes the player unable to walk
                player.setWalkSpeed(0.0f);

                player.setInvulnerable(true);

                return true;
            }
            if (teamManager.isGamePaused()) {
                sender.sendMessage("Game is already paused");
                return true;
            }
        } else {
            sender.sendMessage("You do not have OP");
            return true;
        }


        return false;
    }
}