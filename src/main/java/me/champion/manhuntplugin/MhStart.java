package me.champion.manhuntplugin;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.Sound;
public class MhStart implements CommandExecutor {

    private final TeamManager teamManager;

    public boolean gameStarted = false;
    private BukkitTask countdownTask;
    private BossBar bossBar;
    public boolean timerExpired = false;
    private boolean initialCountdownInProgress = false;
    private BukkitTask initialCountdownTask;

    public MhStart(TeamManager teamManager) {

        this.teamManager = teamManager;
    }
    public BossBar getBossBar() {
        return this.bossBar;
    }

    public boolean isGameStarted() {
        return gameStarted;
    }

    public void resetGame() {
        cancelInitialCountdown();

        if (countdownTask != null && !countdownTask.isCancelled()) {
            countdownTask.cancel();
            countdownTask = null;
        }

        hideBossBar();
        gameStarted = false;
        timerExpired = false;
        teamManager.unpauseZombies();
    }

    public void hideBossBar() {
        if (bossBar != null) {
            bossBar.setVisible(false);
            bossBar.removeAll();
            if (countdownTask != null && !countdownTask.isCancelled()) {
                countdownTask.cancel();
                countdownTask = null;
            }
        }
    }

    public void cancelInitialCountdown() {
        if (initialCountdownTask != null && !initialCountdownTask.isCancelled()) {
            initialCountdownTask.cancel();
            initialCountdownInProgress = false;
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!player.isOp() && !gameStarted) {
            player.setGameMode(GameMode.ADVENTURE);
        }
        if (gameStarted) {
            System.out.println("cheese");
            Bukkit.getScheduler().runTaskLater(Manhunt.getPlugin(), () -> {
                // Code to execute after the delay

                // For example, send a welcome message to the player after 1 second
                bossBar.setVisible(true); // Make the boss bar visible after the initial countdown
                bossBar.addPlayer(player);

            }, 20L); // 20 ticks = 1 second (1 tick = 0.05 seconds)
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

        for (Player onlineplayer : Bukkit.getOnlinePlayers()) {
            onlineplayer.setGameMode(GameMode.SURVIVAL);

            onlineplayer.setHealth(20.0);

            onlineplayer.setFoodLevel(20);

            onlineplayer.setSaturation(20);

            onlineplayer.playSound(onlineplayer.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, 1.0F);

            onlineplayer.setInvulnerable(false);
        }
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "effect clear @a");

        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "time set 0");

        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "gamerule domobspawning true");

        MhCreate mhCreate = new MhCreate(Manhunt.getPlugin(), teamManager,this, new MhIso(teamManager, Manhunt.getPlugin()));
        mhCreate.removeGlassSphere((Player) sender);

        //resetBossBar();
        gameStarted = true;
        teamManager.pauseZombies();
        startInitialCountdown();
        giveCompassesToZombies();
        return true;
    }

    private void startInitialCountdown() {
        if (initialCountdownTask != null && !initialCountdownTask.isCancelled()) {
            initialCountdownTask.cancel();
        }

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

                    createAndStartBossBar(); // Start the boss bar timer after the initial countdown
                } else {
                    Bukkit.broadcastMessage("§cZombies §fcan move in " + secondsLeft + " seconds!");
                }
                secondsLeft--;
            }
        }.runTaskTimer(Manhunt.getPlugin(), 0L, 20L);
    }

    private void createAndStartBossBar() {
        if (bossBar != null) {
            bossBar.removeAll(); // Reset the boss bar for a new game
        }
        bossBar = Bukkit.createBossBar("Game Timer", BarColor.PURPLE, BarStyle.SEGMENTED_10);
        bossBar.setVisible(true); // Make the boss bar visible after the initial countdown

        long totalSeconds = 2 * 3600 + 29 * 60 + 50; // 2 hours, 29 minutes, and 50 seconds
        //long totalSeconds = 5;
        countdownTask = new BukkitRunnable() {
            long secondsLeft = totalSeconds;

            @Override
            public void run() {
                if (secondsLeft <= 0) {
                    bossBar.setVisible(false);
                    timerExpired = true;
                    this.cancel();
                    return;
                }

                double progress = (double) secondsLeft / totalSeconds;
                bossBar.setProgress(progress);
                bossBar.setTitle(formatTime(secondsLeft));
                if (!teamManager.isGamePaused()) {
                    secondsLeft--;
                }
            }
        }.runTaskTimer(Manhunt.getPlugin(), 0L, 20L);

        Bukkit.getOnlinePlayers().forEach(bossBar::addPlayer);
    }

    private String formatTime(long totalSeconds) {
        int hours = (int) totalSeconds / 3600;
        int minutes = (int) (totalSeconds % 3600) / 60;
        int seconds = (int) totalSeconds % 60;
        return String.format("%dh %dm %ds", hours, minutes, seconds);
    }

    public void giveCompassesToZombies() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (teamManager.isOnTeam(player, "Zombies")) {
                MhCompass compassManager = new MhCompass(teamManager, Manhunt.getPlugin());
                compassManager.giveRunnerCompass(player);
            }
        }
    }
}
