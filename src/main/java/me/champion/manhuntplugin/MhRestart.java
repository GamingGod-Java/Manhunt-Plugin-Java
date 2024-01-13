package me.champion.manhuntplugin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class MhRestart implements CommandExecutor {

    private final MhStart mhStart;

    public MhRestart(MhStart mhStart) {
        this.mhStart = mhStart;
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
        } else {
            sender.sendMessage("There is no game to restart.");
        }

        return true;
    }
}
