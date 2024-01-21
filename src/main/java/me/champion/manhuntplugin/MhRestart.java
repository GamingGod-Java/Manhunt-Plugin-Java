package me.champion.manhuntplugin;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MhRestart implements CommandExecutor {

    private final MhStart mhStart;
    private final TeamManager teamManager;

    public MhRestart(MhStart mhStart, TeamManager teamManager) {
        this.mhStart = mhStart;
        this.teamManager = teamManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage("§cYou do not have permission to use this command.");
            return true;
        } else {
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.getInventory().clear();
            }
            mhStart.resetGame(); // Reset game, including both countdowns and boss bar
            // Broadcast message to all players
            Bukkit.broadcastMessage("The game has been reset.");
            teamManager.unpauseZombies();
        }


        return true;
    }
}
