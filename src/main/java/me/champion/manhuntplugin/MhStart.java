package me.champion.manhuntplugin;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class MhStart implements CommandExecutor {

    private final TeamManager teamManager;
    private boolean gameStarted = false;
    private BukkitTask countdownTask; // To keep track of the countdown task

    public MhStart(TeamManager teamManager) {
        this.teamManager = teamManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player) || !sender.isOp()) {
            sender.sendMessage("You do not have permission to use this command.");
            return true;
        }

        if (gameStarted) {
            sender.sendMessage("The game has already started. Use /mhrestart to restart.");
        } else {
            gameStarted = true;
            teamManager.pauseZombies();
            startCountdown();
            sender.sendMessage("Game started. Team selection is disabled.");
        }

        return true;
    }

    public boolean isGameStarted() {
        return gameStarted;
    }

    private void startCountdown() {
        BukkitTask initialCountdownTask = new BukkitRunnable() {
            int secondsLeft = 10;

            @Override
            public void run() {
                if (secondsLeft > 0) {
                    Bukkit.broadcastMessage("Game starts in " + secondsLeft + " seconds!");
                } else {
                    Bukkit.broadcastMessage("The zombies can now move!");
                    teamManager.unpauseZombies();
                    this.cancel(); // Cancel the initial countdown task
                }
                secondsLeft--;
            }
        }.runTaskTimer(Manhunt.getPlugin(), 0L, 20L); // Update every second (20 ticks = 1 second)

        countdownTask = initialCountdownTask; // Store the initial countdown task
    }

    public void resetGame() {
        if (countdownTask != null && !countdownTask.isCancelled()) {
            countdownTask.cancel();
        }
        gameStarted = false;
    }
}
