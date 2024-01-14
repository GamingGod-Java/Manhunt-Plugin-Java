package me.champion.manhuntplugin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class MhRestart implements CommandExecutor {

    private final MhStart mhStart;
    private final TeamManager teamManager;
    public MhRestart(MhStart mhStart,TeamManager teamManager) {
        this.mhStart = mhStart;
        this.teamManager = teamManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage("You do not have permission to use this command.");
            return true;
        }

        if (mhStart.isGameStarted()) {
            mhStart.resetGame();
            sender.sendMessage("Game restarted.");
            teamManager.unpauseZombies();
        } else {
            sender.sendMessage("There is no game to restart.");
        }

        return true;
    }
}