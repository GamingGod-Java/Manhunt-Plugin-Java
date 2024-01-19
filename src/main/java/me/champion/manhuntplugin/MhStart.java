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
    public boolean timerExpired = false;
    private BukkitTask countdownTask;
    private BossBar bossBar;
    private boolean initialCountdownInProgress = false;
    private BukkitTask initialCountdownTask;

    public MhStart(TeamManager teamManager) {
        this.teamManager = teamManager; // Store the TeamManager instance
    }

    public boolean isGameStarted() {
        return gameStarted;
    }

    public void resetGame() {
        cancelInitialCountdown(); // Cancel initial countdown if in progress

        if (countdownTask != null && !countdownTask.isCancelled()) {
            countdownTask.cancel();
        }
        resetBossBar();
        gameStarted = false;
        teamManager.unpauseZombies(); // Make sure to reset zombies' state when the game is reset
    }

    public boolean isInitialCountdownInProgress() {
        return initialCountdownInProgress;
    }

    public void cancelInitialCountdown() {
        if (initialCountdownTask != null && !initialCountdownTask.isCancelled()) {
            initialCountdownTask.cancel();
            initialCountdownInProgress = false;
            teamManager.unpauseZombies(); // Make zombies vulnerable as countdown is canceled
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player) || !sender.isOp()) {
            sender.sendMessage("§cYou do not have permission to use this command.");
            return true;
        }

        if (gameStarted) {
            sender.sendMessage("§cThe game has already started. Use /mhrestart to restart.");
            return true;
        }

        // Remove the glass sphere
        MhCreate mhCreate = new MhCreate(Manhunt.getPlugin(), teamManager);
        mhCreate.removeGlassSphere((Player) sender);

        resetBossBar(); // Reset any existing boss bar

        gameStarted = true;
        teamManager.pauseZombies(); // Make zombies invincible for the countdown duration
        startInitialCountdown();
        createAndStartBossBar();
        Bukkit.broadcastMessage("Starting game, disabling team selection");
        // Give compasses to all players on the Zombies team
        giveCompassesToZombies();
        return true;
    }
    private void startInitialCountdown() {
        initialCountdownInProgress = true;
        initialCountdownTask = new BukkitRunnable() {
            int secondsLeft = 10;

            @Override
            public void run() {
                if (secondsLeft <= 0) {
                    Bukkit.broadcastMessage("The §cZombies §fcan now move!");
                    teamManager.unpauseZombies();
                    this.cancel();
                    initialCountdownInProgress = false;
                    createAndStartBossBar(); // Create and start boss bar countdown here
                    if (bossBar != null) {
                        bossBar.setVisible(true); // Make boss bar visible
                    }
                } else {
                    Bukkit.broadcastMessage("§cZombies §fcan move in " + secondsLeft + " seconds!");
                }
                secondsLeft--;
            }
        }.runTaskTimer(Manhunt.getPlugin(), 0L, 20L);
    }
    private void createAndStartBossBar() {
        long totalSeconds = 2 * 3600 + 30 * 60; // 2 hours and 30 minutes
        long initialOffset = 10; // 10 seconds for the initial countdown
        bossBar = Bukkit.createBossBar("Game Timer", BarColor.PURPLE, BarStyle.SEGMENTED_10);
        bossBar.setVisible(false); // Initially hide the boss bar

        countdownTask = new BukkitRunnable() {
            long secondsLeft = totalSeconds - initialOffset; // Start from 2h 29m 50s

            @Override
            public void run() {
                if (secondsLeft <= 0) {
                    bossBar.setVisible(false);
                    this.cancel();
                    return;
                }

                double progress = (double) secondsLeft / totalSeconds;
                bossBar.setProgress(progress);

                String timeFormatted = formatTime(secondsLeft);
                bossBar.setTitle(timeFormatted);

                if (!teamManager.isGamePaused()) {
                    secondsLeft--;
                }
            }
        }.runTaskTimer(Manhunt.getPlugin(), 0L, 20L);

        Bukkit.getOnlinePlayers().forEach(bossBar::addPlayer);
    }

    // Helper method to format time
    private String formatTime(long totalSeconds) {
        int hours = (int) totalSeconds / 3600;
        int minutes = (int) (totalSeconds % 3600) / 60;
        int seconds = (int) totalSeconds % 60;
        return String.format("%dh %dm %ds", hours, minutes, seconds);
    }

    private void resetBossBar() {
        if (bossBar != null) {
            bossBar.removeAll();
            bossBar.setVisible(false);
            bossBar = null; // Dereference the old boss bar
        }
    }

    // Give compasses to all players on the Zombies team
    public void giveCompassesToZombies() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (teamManager.isOnTeam(player, "Zombies")) {
                MhCompass compassManager = new MhCompass(teamManager, Manhunt.getPlugin());
                compassManager.giveRunnerCompass(player);
            }
        }
    }
}
