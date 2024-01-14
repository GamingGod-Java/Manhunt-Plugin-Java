package me.champion.manhuntplugin;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class MhStart implements CommandExecutor {

    private final TeamManager teamManager;
    private boolean gameStarted = false;
    private BukkitTask countdownTask;
    private BossBar bossBar;

    public MhStart(TeamManager teamManager) {
        this.teamManager = teamManager;
    }

    public boolean isGameStarted() {
        return gameStarted;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player) || !sender.isOp()) {
            sender.sendMessage("You do not have permission to use this command.");
            return true;
        }

        if (gameStarted) {
            sender.sendMessage("The game has already started. Use /mhrestart to restart.");
            return true;
        }

        resetBossBar(); // Reset any existing boss bar

        gameStarted = true;
        teamManager.pauseZombies();
        startCountdown();
        createAndStartBossBar();
        sender.sendMessage("Game started. Team selection is disabled.");

        return true;
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

    private void createAndStartBossBar() {
        long totalSeconds = 2 * 3600 + 30 * 60; // 2 hours and 30 minutes
        bossBar = Bukkit.createBossBar("Game Timer", BarColor.PURPLE, BarStyle.SEGMENTED_10);
        bossBar.setVisible(false); // Initially hide the boss bar

        countdownTask = new BukkitRunnable() {
            long secondsLeft = totalSeconds;
            boolean bossBarVisible = false;
            int initialDelay = 10; // 10 seconds delay

            @Override
            public void run() {
                if (secondsLeft <= 0) {
                    bossBar.setVisible(false);
                    this.cancel();
                    return;
                }

                if (!bossBarVisible && initialDelay <= 0) {
                    bossBar.setVisible(true);
                    bossBarVisible = true;
                }

                if (WinCondition.endEntered) {
                    if (bossBar != null) {
                        bossBar.setVisible(false);
                        bossBarVisible = false;
                        this.cancel();
                        resetBossBar();
                    }
                    return;
                }

                double progress = (double) secondsLeft / totalSeconds;
                bossBar.setProgress(progress);

                int hours = (int) secondsLeft / 3600;
                int minutes = (int) (secondsLeft % 3600) / 60;
                int seconds = (int) secondsLeft % 60;
                String timeFormatted = String.format("%dh %dm %ds", hours, minutes, seconds);
                bossBar.setTitle(timeFormatted);

                secondsLeft--;
                initialDelay--;
            }
        }.runTaskTimer(Manhunt.getPlugin(), 0L, 20L); // Update every second

        Bukkit.getOnlinePlayers().forEach(bossBar::addPlayer);
    }

    private void resetBossBar() {
        if (bossBar != null) {
            bossBar.removeAll();
            bossBar.setVisible(false);
            bossBar = null; // Dereference the old boss bar
        }
    }

    public void resetGame() {
        if (countdownTask != null && !countdownTask.isCancelled()) {
            countdownTask.cancel();
        }
        resetBossBar();
        gameStarted = false;
    }
}